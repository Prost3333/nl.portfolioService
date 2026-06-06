package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(BigDecimal totalValue,
                                       List<PositionValue> positions) {
}
