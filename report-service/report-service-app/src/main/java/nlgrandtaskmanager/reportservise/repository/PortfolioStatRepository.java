package nlgrandtaskmanager.reportservise.repository;

import nlgrandtaskmanager.reportservise.model.PortfolioStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PortfolioStatRepository extends JpaRepository<PortfolioStat, UUID> {
}
