package nlgrandtaskmanager.portfolio_service.repository;

import nlgrandtaskmanager.portfolio_service.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateAsc(UUID userId);
    boolean existsByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);
    List<PortfolioSnapshot> findByUserIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
            UUID userId, LocalDate fromDate);
}
