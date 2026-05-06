package nlgrandtaskmanager.financesevice.service;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.financesevice.model.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;


    public void publish(TransactionEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.getUserId().toString(),
                event
        );
    }

    public void publishUpdatedEvent(
            Transaction transaction,
            TransactionType oldType,
            BigDecimal oldAmount
    ) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .userId(transaction.getUserId())

                .oldType(oldType)
                .oldAmount(oldAmount)

                .newType(transaction.getType())
                .newAmount(transaction.getAmount())

                .eventType(TransactionEventType.UPDATED)
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send("transactions", event);
    }

}

