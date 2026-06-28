package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;

public record PositionValue(String ticker,
                            String name,
                            BigDecimal quantity,
                            BigDecimal price,
                            BigDecimal value,
                            BigDecimal allocation,
                            boolean priceAvailable
) {
}
