package nlgrandtaskmanager.portfolio_service.controller;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.PerformanceItem;
import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.SnapshotResponse;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import nlgrandtaskmanager.portfolio_service.service.TradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final TradeService tradeService;

    @GetMapping("/summary")
    public PortfolioSummaryResponse getSummary(Authentication authentication){
        UUID uuid=(UUID)authentication.getPrincipal();
        return portfolioService.getSummary(uuid);
    }
    @GetMapping("/history")
    public List<SnapshotResponse> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "all") String period) {
        UUID userId = (UUID) authentication.getPrincipal();
        return portfolioService.getHistory(userId, period);
    }

    @PostMapping("/snapshot")
    public ResponseEntity<Void> createSnapshot(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        portfolioService.saveSnapshot(userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Разовое восстановление истории по журналу сделок: пересобирает позиции и достраивает
     * снимки задним числом. Нужен после импорта сделок мимо API (Liquibase-миграция).
     * Вызывать повторно безопасно — уже существующие снимки не трогаются.
     */
    @PostMapping("/backfill-history")
    public Map<String, Integer> backfillHistory(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return Map.of(
                "positions", tradeService.rebuildPositions(userId),
                "snapshots", portfolioService.backfillHistory(userId));
    }

    @GetMapping("/admin/snapshots/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SnapshotResponse> getAnySnapshots(@PathVariable UUID userId) {
        return portfolioService.getHistory(userId, "all");
    }

    @GetMapping("/performance")
    public List<PerformanceItem> getPerformance(
            Authentication authentication,
            @RequestParam(defaultValue = "month") String period) {
        UUID userId = (UUID) authentication.getPrincipal();
        return portfolioService.getPerformance(userId, period);
    }

    @GetMapping("/getPercent")
    public BigDecimal getPercentChanges(Authentication authentication,
                                                        @RequestParam(defaultValue = "all") String period){
        UUID userId = (UUID) authentication.getPrincipal();
        return portfolioService.getPercentChanges(userId,period);
    }
}
