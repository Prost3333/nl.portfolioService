package nlgrandtaskmanager.portfolio_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.CreateTradeRequest;
import nlgrandtaskmanager.portfolio_service.dto.TickerInfo;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;
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
    }
}

