package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.model.TransactionEvent;
import nlgrandtaskmanager.reportservise.model.TransactionType;
import nlgrandtaskmanager.reportservise.model.UserBalance;
import nlgrandtaskmanager.reportservise.repository.UserBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@RequiredArgsConstructor
public class UserBalanceService {

    private final UserBalanceRepository repository;

    @Transactional
    public void apply(TransactionEvent event) {
        UserBalance balance = repository.findById(event.userId())
                .orElseGet(() -> UserBalance.builder()
                        .userId(event.userId())
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .build());

        BigDecimal amount = event.amount();

        switch (event.eventType()) {
            case CREATED -> applyCreate(balance, event.type(), amount);
            case DELETED -> applyDelete(balance, event.type(), amount);
        }

        balance.setUpdatedAt(Instant.now());
        repository.save(balance);
    }

    private void applyCreate(UserBalance balance, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            balance.setTotalIncome(balance.getTotalIncome().add(amount));
        } else {
            balance.setTotalExpense(balance.getTotalExpense().add(amount));
        }
    }

    private void applyDelete(UserBalance balance, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.INCOME) {
            balance.setTotalIncome(balance.getTotalIncome().subtract(amount));
        } else {
            balance.setTotalExpense(balance.getTotalExpense().subtract(amount));
        }
    }
}

