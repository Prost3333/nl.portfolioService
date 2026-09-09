package nlgrandtaskmanager.ai_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nlgrandtaskmanager.ai_service.dto.TradeCreatedEvent;
import nlgrandtaskmanager.ai_service.model.TradeRecord;
import nlgrandtaskmanager.ai_service.repository.TradeRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventListener {

    private final TradeRecordRepository tradeRecordRepository;

    /**
     * Первичный ключ read-model — id сделки из portfolio-service, поэтому повторная
     * доставка того же события просто перезаписывает строку теми же данными.
     */
    @KafkaListener(topics = "trade-events", groupId = "ai-service")
    public void onTradeCreated(TradeCreatedEvent event) {
        tradeRecordRepository.save(TradeRecord.builder()
                .tradeId(event.tradeId())
                .userId(event.userId())
                .ticker(event.ticker())
                .type(event.type())
                .quantity(event.quantity())
                .price(event.price())
                .tradeDate(event.tradeDate())
                .rationale(event.rationale())
                .conviction(event.conviction())
                .emotion(event.emotion())
                .receivedAt(Instant.now())
                .build());

        log.info("Сохранена сделка {} {} для пользователя {}",
                event.type(), event.ticker(), event.userId());
    }
}
