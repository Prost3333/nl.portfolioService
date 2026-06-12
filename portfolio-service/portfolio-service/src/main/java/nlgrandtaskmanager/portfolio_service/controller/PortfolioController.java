package nlgrandtaskmanager.portfolio_service.controller;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.SnapshotResponse;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

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

    @GetMapping("/admin/snapshots/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SnapshotResponse> getAnySnapshots(@PathVariable UUID userId) {
        return portfolioService.getHistory(userId, "all");
    }
}
