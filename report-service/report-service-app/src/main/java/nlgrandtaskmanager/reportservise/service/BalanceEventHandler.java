package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.model.TransactionEvent;
import nlgrandtaskmanager.reportservise.model.TransactionType;
import nlgrandtaskmanager.reportservise.model.TransactionUpdatedEvent;
import nlgrandtaskmanager.reportservise.model.UserBalance;
import nlgrandtaskmanager.reportservise.repository.UserBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceEventHandler {

    private final UserBalanceRepository repository;

    // CREATED
    @Transactional
    public void handleCreated(TransactionEvent event) {

        UserBalance balance = getOrCreate(event.userId());


        BigDecimal amount = event.amount() != null ? event.amount() : BigDecimal.ZERO;

        if (event.type() == TransactionType.INCOME) {
            BigDecimal currentIncome = Objects.requireNonNullElse(balance.getTotalIncome(), BigDecimal.ZERO);
            balance.setTotalIncome(currentIncome.add(amount));
        } else {
            BigDecimal currentExpense = Objects.requireNonNullElse(balance.getTotalExpense(), BigDecimal.ZERO);
            balance.setTotalExpense(currentExpense.add(amount));
        }

        balance.setUpdatedAt(Instant.now());
        repository.save(balance);
    }


    public void handleDeleted(TransactionEvent event) {
        UserBalance balance = getOrCreate(event.userId());

        rollback(balance, event.type(), event.amount());

        repository.save(balance);
    }


    public void handleUpdated(TransactionUpdatedEvent event) {
        UserBalance balance = getOrCreate(event.userId());

        rollback(balance, event.oldType(), event.oldAmount());

        apply(balance, event.newType(), event.newAmount());

        repository.save(balance);
    }


    private UserBalance getOrCreate(UUID userId) {
        return repository.findById(userId)
                .orElseGet(() -> UserBalance.builder()
                        .userId(userId)
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .updatedAt(Instant.now())
                        .build());
    }

    private void apply(UserBalance balance, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            balance.setTotalIncome(balance.getTotalIncome().add(amount));
        } else {
            balance.setTotalExpense(balance.getTotalExpense().add(amount));
        }
        balance.setUpdatedAt(Instant.now());
    }

    private void rollback(UserBalance balance, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            balance.setTotalIncome(balance.getTotalIncome().subtract(amount));
        } else {
            balance.setTotalExpense(balance.getTotalExpense().subtract(amount));
        }
        balance.setUpdatedAt(Instant.now());
    }
}
