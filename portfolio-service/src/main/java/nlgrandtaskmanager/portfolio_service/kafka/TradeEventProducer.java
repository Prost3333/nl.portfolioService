package nlgrandtaskmanager.portfolio_service.kafka;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.TradeCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "trade-events";

    public void publish(TradeCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.userId().toString(), event);
    }
}
