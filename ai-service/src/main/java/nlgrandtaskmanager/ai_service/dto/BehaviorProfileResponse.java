package nlgrandtaskmanager.ai_service.dto;

import nlgrandtaskmanager.ai_service.model.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record BehaviorProfileResponse(UUID id,
                                      ProfileStatus status,
                                      int tradesAnalyzed,
                                      String model,
                                      Instant createdAt,
                                      Instant completedAt,
                                      BehaviorMetrics metrics,
                                      BehaviorAnalysis analysis,
                                      String error) {
}
