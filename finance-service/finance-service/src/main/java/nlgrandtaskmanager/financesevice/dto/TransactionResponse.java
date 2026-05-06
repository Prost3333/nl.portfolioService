package nlgrandtaskmanager.financesevice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import nlgrandtaskmanager.financesevice.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        @NotNull
        UUID id,
        @NotNull
        @Positive
        BigDecimal amount,
        @NotNull
        TransactionType type,
        @NotBlank
        @Size(max = 50)
        String category,
        String description,
        Instant createdAt) {
}
