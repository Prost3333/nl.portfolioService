package nlgrandtaskmanager.portfolio_service.repository;


import nlgrandtaskmanager.portfolio_service.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByUserIdAndTicker(UUID userId, String ticker);
}
