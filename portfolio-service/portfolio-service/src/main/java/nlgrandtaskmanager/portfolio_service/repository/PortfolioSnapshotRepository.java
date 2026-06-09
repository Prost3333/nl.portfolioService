package nlgrandtaskmanager.portfolio_service.repository;

import nlgrandtaskmanager.portfolio_service.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateAsc(UUID userId);
    boolean existsByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);
}
