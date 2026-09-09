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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
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

        List<Trade> buys = trades.stream()
                .filter(t -> t.getType() == TradeType.BUY)
                .toList();

        BigDecimal totalQuantity = buys.stream()
                .map(Trade::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = buys.stream()
                .map(t -> t.getPrice().multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePrice = totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);

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

