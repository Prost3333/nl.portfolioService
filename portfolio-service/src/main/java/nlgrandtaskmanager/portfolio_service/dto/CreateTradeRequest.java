package nlgrandtaskmanager.portfolio_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTradeRequest(
        @NotBlank String ticker,
        @NotNull @Positive BigDecimal quantity,
        @NotNull TradeType type,
        @NotNull @Positive BigDecimal price,
        LocalDate tradeDate
) {

    public CreateTradeRequest {
        if (tradeDate == null) {
            tradeDate = LocalDate.now();
        }
    }
}
