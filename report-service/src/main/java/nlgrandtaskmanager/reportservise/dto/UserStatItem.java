package nlgrandtaskmanager.reportservise.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UserStatItem(
        UUID userId,
        BigDecimal lastValue,
        LocalDate lastSnapshotDate,
        int snapshotCount
) {}
