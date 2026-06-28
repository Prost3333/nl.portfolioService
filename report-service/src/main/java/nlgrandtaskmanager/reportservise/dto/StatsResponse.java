package nlgrandtaskmanager.reportservise.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StatsResponse (int totalUsers,
                             BigDecimal totalValueAllUsers,
                             List<UserStatItem> users){
}


