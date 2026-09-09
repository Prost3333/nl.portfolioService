package nlgrandtaskmanager.ai_service.repository;

import nlgrandtaskmanager.ai_service.model.BehaviorProfile;
import nlgrandtaskmanager.ai_service.model.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BehaviorProfileRepository extends JpaRepository<BehaviorProfile, UUID> {

    Optional<BehaviorProfile> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Ограничение по времени нужно, чтобы упавший процесс не оставил вечный PENDING,
     * который навсегда заблокирует пользователю повторный анализ.
     */
    boolean existsByUserIdAndStatusAndCreatedAtAfter(UUID userId, ProfileStatus status, Instant after);
}
