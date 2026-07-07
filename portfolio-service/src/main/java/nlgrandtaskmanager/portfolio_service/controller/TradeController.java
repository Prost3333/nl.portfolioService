package nlgrandtaskmanager.portfolio_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.CreateTradeRequest;
import nlgrandtaskmanager.portfolio_service.service.TradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService tradeService;

    @PostMapping("/trades")
    public ResponseEntity<Void> addTrade(
            Authentication authentication,
            @Valid @RequestBody CreateTradeRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        tradeService.addTrade(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
