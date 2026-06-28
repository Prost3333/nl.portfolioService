package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;

public record PerformanceItem(String ticker,
                              String name,
                              BigDecimal changePercent,
                              boolean available) {
}
