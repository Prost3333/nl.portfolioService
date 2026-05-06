package nlgrandtaskmanager.reportservise.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionUpdatedEvent(
        UUID eventId,
        UUID transactionId,
        UUID userId,

        TransactionType oldType,
        BigDecimal oldAmount,

        TransactionType newType,
        BigDecimal newAmount,

        TransactionEventType eventType,
        Instant occurredAt
) {}

