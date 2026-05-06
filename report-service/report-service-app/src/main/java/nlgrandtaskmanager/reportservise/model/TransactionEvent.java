package nlgrandtaskmanager.reportservise.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID eventId,
        UUID transactionId,
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        TransactionEventType eventType,
        Instant occurredAt
) {}


