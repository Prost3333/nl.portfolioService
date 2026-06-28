package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SnapshotResponse(LocalDate date,
                               BigDecimal totalValue) {
}
