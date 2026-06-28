package nlgrandtaskmanager.reportservise.controller;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.dto.StatsResponse;
import nlgrandtaskmanager.reportservise.service.PortfolioStatService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final PortfolioStatService portfolioStatService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsResponse getStat(){
       return portfolioStatService.getStats();
    }
}
