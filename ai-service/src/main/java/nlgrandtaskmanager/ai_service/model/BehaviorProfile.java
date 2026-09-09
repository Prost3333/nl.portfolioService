package nlgrandtaskmanager.ai_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "behavior_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileStatus status;

    @Column(name = "trades_analyzed", nullable = false)
    private int tradesAnalyzed;

    /** Детерминированные метрики, посчитанные кодом — вход для модели и одновременно то, что показываем в UI. */
    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    /** Интерпретация от Claude в виде JSON (summary / patterns / strengths / recommendations). */
    @Column(name = "analysis_json", columnDefinition = "TEXT")
    private String analysisJson;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
