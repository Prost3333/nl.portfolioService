package nlgrandtaskmanager.reportservise.service;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.dto.SnapshotCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotEventListener {

    private final PortfolioStatService statService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "snapshot-events", groupId = "report-service")
    public void onSnapshotCreated(SnapshotCreatedEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.eventId())) {
            return;
        }

        statService.handleSnapshot(event);
        idempotencyService.markProcessed(event.eventId());
    }
}
