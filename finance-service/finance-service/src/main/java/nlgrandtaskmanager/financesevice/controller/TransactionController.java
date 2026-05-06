package nlgrandtaskmanager.financesevice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.financesevice.dto.BalanceResponse;
import nlgrandtaskmanager.financesevice.dto.TransactionResponse;
import nlgrandtaskmanager.financesevice.dto.CreateTransactionRequest;
import nlgrandtaskmanager.financesevice.dto.TransactionSummary;
import nlgrandtaskmanager.financesevice.model.TransactionType;
import nlgrandtaskmanager.financesevice.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @GetMapping("/test")
    public String test(Authentication authentication) {
        return "Hello user " + authentication.getPrincipal();
    }

    @PostMapping
    public TransactionResponse create(
            @RequestBody @Valid CreateTransactionRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return service.create(userId, request);
    }


    @GetMapping("/summary")
    public TransactionSummary summary(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return service.getSummary(userId);
    }

    @GetMapping("/getBalance")
    public BalanceResponse getBalance(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return service.getBalance(userId);
    }

    @GetMapping("/search")
    public Page<TransactionResponse> search(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return service.search(
                userId, from, to, type, category, pageable
        );
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        service.delete(userId, id);
    }


    @PutMapping("/{id}")
    public TransactionResponse update(
            Authentication authentication,
            @PathVariable("id") UUID transactionId,
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        return service.update(use
                rId, transactionId, request);
    }


}
