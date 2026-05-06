package nlgrandtaskmanager.financesevice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import nlgrandtaskmanager.financesevice.model.TransactionType;

import java.math.BigDecimal;
@Getter
@Setter
public class CreateTransactionRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @NotBlank
    @Size(max = 50)
    private String category;
    @Size(max = 255)
    private String description;
}
