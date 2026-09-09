package nlgrandtaskmanager.portfolio_service.dto;

import nlgrandtaskmanager.portfolio_service.enums.TradeEmotion;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TradeResponse(UUID id,
                            String ticker,
                            TradeType type,
                            BigDecimal quantity,
                            BigDecimal price,
                            LocalDate tradeDate,
                            String rationale,
                            Integer conviction,
                            TradeEmotion emotion) {
}
