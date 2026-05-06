package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.model.TransactionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final IdempotencyService idempotencyService;
    private final BalanceEventHandler balanceEventHandler;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "report-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(TransactionEvent event) {

        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            return;
        }

        switch (event.eventType()) {
            case CREATED -> balanceEventHandler.handleCreated(event);
            case DELETED -> balanceEventHandler.handleDeleted(event);
            default -> {
                // ignore
            }
        }

        idempotencyService.markProcessed(event.eventId());
    }
}


