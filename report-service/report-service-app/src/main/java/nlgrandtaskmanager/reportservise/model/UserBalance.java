package nlgrandtaskmanager.reportservise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_balance")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserBalance {

    @Id
    @NotNull
    private UUID userId;
    @Builder.Default
    @Column(nullable = false)
    private BigDecimal totalIncome = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant updatedAt;

    public UserBalance(UUID userId) {
        this.userId = userId;
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpense = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        if (updatedAt == null) updatedAt = Instant.now();
    }
}
