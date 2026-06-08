package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.PositionValue;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PortfolioService portfolioService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getSummary_returnsEmptyPortfolio_whenUserHasNoPositions() {
        when(positionRepository.findByUserId(userId)).thenReturn(List.of());

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        assertThat(result.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.positions()).isEmpty();
    }

    @Test
    void getSummary_calculatesValueCorrectly_whenAllPricesAvailable() {
        Position aapl = buildPosition("AAPL", "Apple", new BigDecimal("10"));
        Position goog = buildPosition("GOOG", "Google", new BigDecimal("5"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(aapl, goog));
        when(priceService.getPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(priceService.getPrice("GOOG")).thenReturn(new BigDecimal("200"));

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        // AAPL: 10 * 100 = 1000, GOOG: 5 * 200 = 1000, total = 2000
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.positions()).hasSize(2);

        PositionValue aaplValue = findByTicker(result, "AAPL");
        assertThat(aaplValue.value()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(aaplValue.price()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(aaplValue.priceAvailable()).isTrue();

        PositionValue googValue = findByTicker(result, "GOOG");
        assertThat(googValue.value()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(googValue.priceAvailable()).isTrue();
    }

    @Test
    void getSummary_calculatesAllocationPercentages() {
        Position aapl = buildPosition("AAPL", "Apple", new BigDecimal("1"));
        Position goog = buildPosition("GOOG", "Google", new BigDecimal("3"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(aapl, goog));
        when(priceService.getPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(priceService.getPrice("GOOG")).thenReturn(new BigDecimal("100"));

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        // AAPL: 100 (25%), GOOG: 300 (75%), total: 400
        PositionValue aaplValue = findByTicker(result, "AAPL");
        assertThat(aaplValue.allocation()).isEqualByComparingTo(new BigDecimal("25"));

        PositionValue googValue = findByTicker(result, "GOOG");
        assertThat(googValue.allocation()).isEqualByComparingTo(new BigDecimal("75"));
    }

    @Test
    void getSummary_setsAllocationToZeroAndTotalToZero_whenPriceIsNull() {
        Position aapl = buildPosition("AAPL", "Apple", new BigDecimal("10"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(aapl));
        when(priceService.getPrice("AAPL")).thenReturn(null);

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        assertThat(result.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.positions()).hasSize(1);

        PositionValue position = result.positions().get(0);
        assertThat(position.priceAvailable()).isFalse();
        assertThat(position.price()).isNull();
        assertThat(position.value()).isNull();
        assertThat(position.allocation()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_handlesPartialPrices_mixedAvailability() {
        Position aapl = buildPosition("AAPL", "Apple", new BigDecimal("10"));
        Position goog = buildPosition("GOOG", "Google", new BigDecimal("5"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(aapl, goog));
        when(priceService.getPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(priceService.getPrice("GOOG")).thenReturn(null);

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        // Only AAPL contributes: 10 * 100 = 1000
        assertThat(result.totalValue()).isEqualByComparingTo(new BigDecimal("1000"));

        PositionValue aaplValue = findByTicker(result, "AAPL");
        assertThat(aaplValue.priceAvailable()).isTrue();
        assertThat(aaplValue.allocation()).isEqualByComparingTo(new BigDecimal("100"));

        PositionValue googValue = findByTicker(result, "GOOG");
        assertThat(googValue.priceAvailable()).isFalse();
    }

    @Test
    void getSummary_returnsZeroTotal_whenAllPricesNull() {
        Position aapl = buildPosition("AAPL", "Apple", new BigDecimal("10"));
        Position goog = buildPosition("GOOG", "Google", new BigDecimal("5"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(aapl, goog));
        when(priceService.getPrice("AAPL")).thenReturn(null);
        when(priceService.getPrice("GOOG")).thenReturn(null);

        PortfolioSummaryResponse result = portfolioService.getSummary(userId);

        assertThat(result.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.positions()).allMatch(p -> !p.priceAvailable());
    }

    private Position buildPosition(String ticker, String name, BigDecimal quantity) {
        return Position.builder()
                .id(UUID.randomUUID()).userId(userId)
                .ticker(ticker).name(name).quantity(quantity)
                .createdAt(Instant.now())
                .build();
    }

    private PositionValue findByTicker(PortfolioSummaryResponse summary, String ticker) {
        return summary.positions().stream()
                .filter(p -> p.ticker().equals(ticker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Position with ticker " + ticker + " not found"));
    }
}
