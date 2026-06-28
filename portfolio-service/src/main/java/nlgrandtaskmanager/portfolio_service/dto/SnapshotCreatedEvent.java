package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SnapshotCreatedEvent(UUID eventId,
                                   UUID userId,
                                   LocalDate snapshotDate,
                                   BigDecimal totalValue) {
}
