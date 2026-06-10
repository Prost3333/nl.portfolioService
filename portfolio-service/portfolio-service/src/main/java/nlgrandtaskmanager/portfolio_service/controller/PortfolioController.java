package nlgrandtaskmanager.portfolio_service.controller;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.SnapshotResponse;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<SnapshotResponse> getHistory(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return portfolioService.getHistory(userId);
    }
}
