package nlgrandtaskmanager.ai_service.controller;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.ai_service.dto.BehaviorProfileResponse;
import nlgrandtaskmanager.ai_service.service.BehaviorAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/behavior")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorAnalysisService analysisService;

    /** Ставит генерацию профиля в очередь и сразу возвращает запись со статусом PENDING. */
    @PostMapping("/analyze")
    public ResponseEntity<BehaviorProfileResponse> analyze(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.accepted().body(analysisService.startAnalysis(userId));
    }

    /** Последний профиль пользователя: PENDING, READY или FAILED. */
    @GetMapping("/profile")
    public ResponseEntity<BehaviorProfileResponse> profile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return analysisService.getLatest(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }
}
