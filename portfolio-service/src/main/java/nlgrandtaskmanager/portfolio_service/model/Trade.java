package nlgrandtaskmanager.portfolio_service.model;

import jakarta.persistence.*;
import lombok.*;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String ticker;
    @Column(nullable = false)
    private BigDecimal quantity;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
}
