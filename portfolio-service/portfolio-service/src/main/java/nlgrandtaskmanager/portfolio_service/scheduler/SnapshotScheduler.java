package nlgrandtaskmanager.portfolio_service.scheduler;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SnapshotScheduler {
    private final PositionRepository positionRepository;
    private final PortfolioService portfolioService;

        @Scheduled(cron = "0 0 18 * * *")
//    @Scheduled(fixedRate = 60000)
    public void takeDailySnapshots() {
        System.out.println(">>> Scheduler make snapshot portfolio");
        for (UUID userId : positionRepository.findDistinctUserIds()) {
            portfolioService.saveSnapshot(userId);
        }
    }
}
