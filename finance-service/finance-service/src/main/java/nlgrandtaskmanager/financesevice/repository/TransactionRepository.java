package nlgrandtaskmanager.financesevice.repository;

import nlgrandtaskmanager.financesevice.model.Transaction;
import nlgrandtaskmanager.financesevice.dto.TransactionSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {


    List<Transaction> findAllByUserId(UUID userId);

    @Query("""
                select new nlgrandtaskmanager.financesevice.dto.TransactionSummary(
                    coalesce(sum(case when t.type = 'INCOME' then t.amount else 0 end), 0),
                    coalesce(sum(case when t.type = 'EXPENSE' then t.amount else 0 end), 0)
                )
                from Transaction t
                where t.userId = :userId
            """)
    TransactionSummary getSummary(UUID userId);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);


}
