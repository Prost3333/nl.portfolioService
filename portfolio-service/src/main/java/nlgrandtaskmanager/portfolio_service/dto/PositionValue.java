package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;

public record PositionValue(String ticker,
                            String name,
                            BigDecimal quantity,
                            BigDecimal price,
                            BigDecimal value,
                            BigDecimal allocation,
                            BigDecimal averagePrice,
                            BigDecimal unrealizedPL,
                            BigDecimal unrealizedPLPercent,
                            boolean priceAvailable
) {

    public PositionValue withAllocation(BigDecimal allocation) {
        return new PositionValue(ticker, name, quantity, price, value, allocation,
                averagePrice, unrealizedPL, unrealizedPLPercent, priceAvailable);
    }
}
