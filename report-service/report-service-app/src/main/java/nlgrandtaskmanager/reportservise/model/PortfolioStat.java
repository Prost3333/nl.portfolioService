package nlgrandtaskmanager.reportservise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portfolio_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioStat {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_value", nullable = false)
    private BigDecimal lastValue;

    @Column(name = "last_snapshot_date", nullable = false)
    private LocalDate lastSnapshotDate;

    @Column(name = "snapshot_count", nullable = false)
    private int snapshotCount;
}