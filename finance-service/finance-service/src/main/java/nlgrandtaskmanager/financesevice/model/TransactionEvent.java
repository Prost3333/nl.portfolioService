package nlgrandtaskmanager.financesevice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEvent {

    private UUID eventId;
    private UUID transactionId;
    private UUID userId;

    // optional (null для CREATED / DELETED)
    private TransactionType oldType;
    private BigDecimal oldAmount;

    // optional (null для DELETED)
    private TransactionType newType;
    private BigDecimal newAmount;

    private TransactionEventType eventType;
    private Instant occurredAt;


}


