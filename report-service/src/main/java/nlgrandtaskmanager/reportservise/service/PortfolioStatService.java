package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.dto.SnapshotCreatedEvent;
import nlgrandtaskmanager.reportservise.dto.StatsResponse;
import nlgrandtaskmanager.reportservise.dto.UserStatItem;
import nlgrandtaskmanager.reportservise.model.PortfolioStat;
import nlgrandtaskmanager.reportservise.repository.PortfolioStatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioStatService {

    private final PortfolioStatRepository repository;

    @Transactional
    public void handleSnapshot(SnapshotCreatedEvent event) {
        PortfolioStat stat = repository.findById(event.userId())
                .orElse(PortfolioStat.builder()
                        .userId(event.userId())
                        .snapshotCount(0)
                        .build());

        stat.setLastValue(event.totalValue());
        stat.setLastSnapshotDate(event.snapshotDate());
        stat.setSnapshotCount(stat.getSnapshotCount() + 1);

        repository.save(stat);
    }

    public StatsResponse getStats() {
        List<PortfolioStat> all = repository.findAll();
        int totalUsers = all.size();
        BigDecimal allValue = all.stream().map(PortfolioStat::getLastValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<UserStatItem> users = all.stream()
                .map(s -> new UserStatItem(
                        s.getUserId(),
                        s.getLastValue(),
                        s.getLastSnapshotDate(),
                        s.getSnapshotCount()
                ))
                .toList();
        return new StatsResponse(totalUsers, allValue, users);
    }
}
