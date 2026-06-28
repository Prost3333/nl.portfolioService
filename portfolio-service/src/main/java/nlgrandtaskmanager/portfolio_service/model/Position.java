package nlgrandtaskmanager.portfolio_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private  String ticker;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private BigDecimal quantity;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
