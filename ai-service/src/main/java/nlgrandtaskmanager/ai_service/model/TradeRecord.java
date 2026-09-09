package nlgrandtaskmanager.ai_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Локальная read-model сделки: наполняется из Kafka-топика trade-events.
 * ai-service никогда не ходит в базу portfolio-service напрямую.
 */
@Entity
@Table(name = "trade_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeRecord {

    /** Совпадает с id сделки в portfolio-service — даёт идемпотентность при переотправке события. */
    @Id
    @Column(name = "trade_id")
    private UUID tradeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType type;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(length = 2000)
    private String rationale;

    private Integer conviction;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TradeEmotion emotion;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
