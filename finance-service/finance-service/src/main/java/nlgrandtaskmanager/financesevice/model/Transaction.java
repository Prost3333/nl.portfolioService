package nlgrandtaskmanager.financesevice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.UUID;

@Entity
@Getter @Setter
@Table(
        name = "transactions",
        indexes = {
                @Index(
                        name = "idx_tx_user_created_at",
                        columnList = "user_id, created_at DESC"
                ),
                @Index(
                        name = "idx_tx_user_category_created_at",
                        columnList = "user_id, category, created_at DESC"
                ),
                @Index(
                        name = "idx_tx_user_type_created_at",
                        columnList = "user_id, type, created_at DESC"
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends AuditableEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String category;

    private String description;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
