package nlgrandtaskmanager.portfolio_service.kafka;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.SnapshotCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotEventProducer {

    private final KafkaTemplate<String, SnapshotCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "snapshot-events";

    public void publish(SnapshotCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.userId().toString(), event);
    }
}
