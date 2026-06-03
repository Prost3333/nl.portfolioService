package nlgrandtaskmanager.portfolio_service.repository;

import nlgrandtaskmanager.portfolio_service.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByUserId(UUID userId);
}
