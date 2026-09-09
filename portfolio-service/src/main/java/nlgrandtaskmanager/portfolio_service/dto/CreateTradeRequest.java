package nlgrandtaskmanager.portfolio_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import nlgrandtaskmanager.portfolio_service.enums.TradeEmotion;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTradeRequest(
        @NotBlank String ticker,
        @NotNull @Positive BigDecimal quantity,
        @NotNull TradeType type,
        @NotNull @Positive BigDecimal price,
        LocalDate tradeDate,
        @Size(max = 2000) String rationale,
        @Min(1) @Max(5) Integer conviction,
        TradeEmotion emotion
) {

    public CreateTradeRequest {
        if (tradeDate == null) {
            tradeDate = LocalDate.now();
        }
        if (rationale != null && rationale.isBlank()) {
            rationale = null;
        }
    }
}
