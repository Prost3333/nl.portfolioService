package nlgrandtaskmanager.portfolio_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.CreateTradeRequest;
import nlgrandtaskmanager.portfolio_service.dto.TickerInfo;
import nlgrandtaskmanager.portfolio_service.dto.TradeCreatedEvent;
import nlgrandtaskmanager.portfolio_service.dto.TradeResponse;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;
import nlgrandtaskmanager.portfolio_service.kafka.TradeEventProducer;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.model.Trade;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import nlgrandtaskmanager.portfolio_service.repository.TradeRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final PriceService priceService;
    private final TradeEventProducer tradeEventProducer;

    @Transactional
    public void addTrade(UUID userId, CreateTradeRequest request) {
        TickerInfo quote = priceService.getQuote(request.ticker());
        if (quote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ticker: " + request.ticker());
        }

        Trade trade = Trade.builder()
                .userId(userId)
                .ticker(request.ticker())
                .quantity(request.quantity())
                .price(request.price())
                .type(request.type())
                .tradeDate(request.tradeDate())
                .createdAt(Instant.now())
                .rationale(request.rationale())
                .conviction(request.conviction())
                .emotion(request.emotion())
                .build();
        tradeRepository.save(trade);

        List<Trade> trades = tradeRepository.findByUserIdAndTicker(userId, request.ticker());

        BigDecimal boughtQuantity = BigDecimal.ZERO;
        BigDecimal boughtCost = BigDecimal.ZERO;
        BigDecimal soldQuantity = BigDecimal.ZERO;

        for (Trade t : trades) {
            if (t.getType() == TradeType.BUY) {
                boughtQuantity = boughtQuantity.add(t.getQuantity());
                boughtCost = boughtCost.add(t.getPrice().multiply(t.getQuantity()));
            } else {
                soldQuantity = soldQuantity.add(t.getQuantity());
            }
        }

        BigDecimal totalQuantity = getBigDecimal(request, boughtQuantity, soldQuantity);

        BigDecimal averagePrice = boughtQuantity.signum() > 0
                ? boughtCost.divide(boughtQuantity, 2, RoundingMode.HALF_UP)
                : null;

        Position position = positionRepository
                .findByUserIdAndTicker(userId, request.ticker())
                .orElseGet(() -> Position.builder()
                        .userId(userId)
                        .ticker(request.ticker())
                        .name(quote.name())
                        .createdAt(Instant.now())
                        .build());

        position.setQuantity(totalQuantity);
        position.setAveragePrice(averagePrice);

        positionRepository.save(position);

        tradeEventProducer.publish(toEvent(trade));
    }

    private static @NonNull BigDecimal getBigDecimal(CreateTradeRequest request, BigDecimal boughtQuantity, BigDecimal soldQuantity) {
        BigDecimal totalQuantity = boughtQuantity.subtract(soldQuantity);

        if (request.type() == TradeType.SELL && totalQuantity.signum() < 0) {
            BigDecimal available = totalQuantity.add(request.quantity());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not enough shares of " + request.ticker()
                            + ": available " + available.toPlainString()
                            + ", requested " + request.quantity().toPlainString());
        }
        return totalQuantity;
    }

    /**
     * Пересобирает позиции из журнала сделок. Нужен после того, как сделки попали в базу
     * мимо {@link #addTrade}, например импортом через Liquibase: там пишется только
     * таблица trades, а positions остаётся с прежними количествами.
     * <p>
     * Трогает только тикеры, которые встречаются в сделках, — позиции, заведённые иначе,
     * остаются как есть.
     *
     * @return сколько позиций создано или обновлено
     */
    @Transactional
    public int rebuildPositions(UUID userId) {
        Map<String, BigDecimal> boughtQuantity = new LinkedHashMap<>();
        Map<String, BigDecimal> boughtCost = new LinkedHashMap<>();
        Map<String, BigDecimal> soldQuantity = new LinkedHashMap<>();

        for (Trade trade : tradeRepository.findByUserIdOrderByTradeDateDesc(userId)) {
            String ticker = trade.getTicker();
            if (trade.getType() == TradeType.BUY) {
                boughtQuantity.merge(ticker, trade.getQuantity(), BigDecimal::add);
                boughtCost.merge(ticker, trade.getPrice().multiply(trade.getQuantity()), BigDecimal::add);
            } else {
                soldQuantity.merge(ticker, trade.getQuantity(), BigDecimal::add);
            }
        }

        for (String ticker : boughtQuantity.keySet()) {
            BigDecimal bought = boughtQuantity.get(ticker);
            BigDecimal sold = soldQuantity.getOrDefault(ticker, BigDecimal.ZERO);

            BigDecimal averagePrice = bought.signum() > 0
                    ? boughtCost.get(ticker).divide(bought, 2, RoundingMode.HALF_UP)
                    : null;

            Position position = positionRepository
                    .findByUserIdAndTicker(userId, ticker)
                    .orElseGet(() -> Position.builder()
                            .userId(userId)
                            .ticker(ticker)
                            .name(resolveName(ticker))
                            .createdAt(Instant.now())
                            .build());

            position.setQuantity(bought.subtract(sold));
            position.setAveragePrice(averagePrice);

            positionRepository.save(position);
        }

        return boughtQuantity.size();
    }

    private String resolveName(String ticker) {
        TickerInfo quote = priceService.getQuote(ticker);
        return quote != null && quote.name() != null ? quote.name() : ticker;
    }

    /**
     * Разовая перезаливка журнала в Kafka. Продюсер появился позже самих сделок,
     * поэтому всё, что добавлено до него, в ai-service не попало.
     * Повторный вызов безопасен: в ai-service первичный ключ строки — это id сделки,
     * так что повторная доставка просто перезаписывает её теми же данными.
     *
     * @return сколько сделок отправлено в топик
     */
    public int republishTrades(UUID userId) {
        List<Trade> trades = tradeRepository.findByUserIdOrderByTradeDateDesc(userId);
        trades.forEach(trade -> tradeEventProducer.publish(toEvent(trade)));
        return trades.size();
    }

    private TradeCreatedEvent toEvent(Trade trade) {
        return new TradeCreatedEvent(
                trade.getId(),
                trade.getUserId(),
                trade.getTicker(),
                trade.getType(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradeDate(),
                trade.getRationale(),
                trade.getConviction(),
                trade.getEmotion()
        );
    }

    public List<TradeResponse> getTrades(UUID userId) {
        return tradeRepository.findByUserIdOrderByTradeDateDesc(userId).stream()
                .map(t -> new TradeResponse(
                        t.getId(),
                        t.getTicker(),
                        t.getType(),
                        t.getQuantity(),
                        t.getPrice(),
                        t.getTradeDate(),
                        t.getRationale(),
                        t.getConviction(),
                        t.getEmotion()))
                .toList();
    }
}

