package nlgrandtaskmanager.ai_service.repository;

import nlgrandtaskmanager.ai_service.model.TradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRecordRepository extends JpaRepository<TradeRecord, UUID> {

    List<TradeRecord> findByUserIdOrderByTradeDateAsc(UUID userId);
}
