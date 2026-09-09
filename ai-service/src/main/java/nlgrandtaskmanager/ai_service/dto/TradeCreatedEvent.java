package nlgrandtaskmanager.ai_service.dto;

import nlgrandtaskmanager.ai_service.model.TradeEmotion;
import nlgrandtaskmanager.ai_service.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TradeCreatedEvent(UUID tradeId,
                                UUID userId,
                                String ticker,
                                TradeType type,
                                BigDecimal quantity,
                                BigDecimal price,
                                LocalDate tradeDate,
                                String rationale,
                                Integer conviction,
                                TradeEmotion emotion) {
}
