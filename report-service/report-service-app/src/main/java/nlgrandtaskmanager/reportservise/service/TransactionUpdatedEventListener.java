package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.model.TransactionUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionUpdatedEventListener {

    private final IdempotencyService idempotencyService;
    private final BalanceEventHandler balanceEventHandler;

    @KafkaListener(
            topics = "transaction-events-updated",
            groupId = "report-service"
    )
    @Transactional
    public void onMessage(TransactionUpdatedEvent event) {

        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            return;
        }

        balanceEventHandler.handleUpdated(event);

        idempotencyService.markProcessed(event.eventId());
    }
}

