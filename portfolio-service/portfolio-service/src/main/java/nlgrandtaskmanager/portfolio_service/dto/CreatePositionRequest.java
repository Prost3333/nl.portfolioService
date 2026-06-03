package nlgrandtaskmanager.portfolio_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePositionRequest(@NotBlank String ticker,
                                    @NotBlank String name,
                                    @NotNull @Positive BigDecimal quantity) {
}
