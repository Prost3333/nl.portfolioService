package nlgrandtaskmanager.portfolio_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.CreatePositionRequest;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.service.PositionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse create(
            @RequestBody @Valid CreatePositionRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return positionService.create(userId, request);
    }

    @GetMapping
    public List<PositionResponse> getPositions(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return positionService.getPositions(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        positionService.delete(userId, id);
    }
}
