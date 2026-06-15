package nlgrandtaskmanager.portfolio_service.scheduler;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;



@Component
@RequiredArgsConstructor
public class SnapshotScheduler {
    private final PositionRepository positionRepository;
    private final PortfolioService portfolioService;
    private static final Logger log =  LoggerFactory.getLogger(SnapshotScheduler.class);

        @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void takeDailySnapshots() {
        log.info(">>> Scheduler make snapshot portfolio");
        for (UUID userId : positionRepository.findDistinctUserIds()) {
            portfolioService.saveSnapshot(userId);
        }
    }
}
