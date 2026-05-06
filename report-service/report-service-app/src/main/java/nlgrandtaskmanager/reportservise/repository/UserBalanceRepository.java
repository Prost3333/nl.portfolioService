package nlgrandtaskmanager.reportservise.repository;

import nlgrandtaskmanager.reportservise.model.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> {
}
