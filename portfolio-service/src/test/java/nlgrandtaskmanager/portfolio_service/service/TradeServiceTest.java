package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.dto.CreateTradeRequest;
import nlgrandtaskmanager.portfolio_service.dto.TickerInfo;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;
import nlgrandtaskmanager.portfolio_service.kafka.TradeEventProducer;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.model.Trade;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import nlgrandtaskmanager.portfolio_service.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {
    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PriceService priceService;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeEventProducer tradeEventProducer;

    @InjectMocks
    private TradeService tradeService;

    private final UUID userId = UUID.randomUUID();


    @Test
    void addTrade_derivesQuantityFromJournal() {
        when(positionRepository.findByUserIdAndTicker(userId, "AAPL")).thenReturn(Optional.of(position()));
        when(priceService.getQuote("AAPL")).thenReturn(new TickerInfo(BigDecimal.valueOf(100),"Apple corp"));
        when(tradeRepository.findByUserIdAndTicker(userId,"AAPL")).thenReturn(List.of(trade()));
        tradeService.addTrade(userId,new CreateTradeRequest("AAPL", BigDecimal.valueOf(2)
                , TradeType.BUY, BigDecimal.valueOf(100), LocalDate.now(), null, null
                , null));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position captured = captor.getValue();
        assertThat(captured.getQuantity()).isEqualByComparingTo("2");
        assertThat(captured.getAveragePrice()).isEqualByComparingTo("100");

    }

    private Position position() {
        return Position.builder().
                userId(userId)
                .ticker("AAPL")
                .quantity(BigDecimal.valueOf(5))
                .averagePrice(BigDecimal.valueOf(100)).build();
    }
    private Trade trade(){
        return Trade.builder().type(TradeType.BUY)
                .quantity(BigDecimal.valueOf(2))
                .price(BigDecimal.valueOf(100)).build();
    }



}
