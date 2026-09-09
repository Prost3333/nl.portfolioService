package nlgrandtaskmanager.ai_service.dto;

import nlgrandtaskmanager.ai_service.model.TradeEmotion;
import nlgrandtaskmanager.ai_service.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Одна сделка с посчитанным рыночным контекстом. Все числа считает код, не модель.
 *
 * @param pricePercentile90d где цена сделки лежала в диапазоне закрытий за 90 дней до неё, 0..100.
 *                           Высокое значение на покупке — вход около локального максимума.
 * @param priorReturn30d     изменение цены за 30 дней ДО сделки, %.
 * @param forwardReturn30d   изменение цены за 30 дней ПОСЛЕ сделки, % (null, если ещё не прошло).
 * @param timingScore        для BUY равен forwardReturn30d, для SELL — со знаком минус.
 *                           Положительный означает, что решение оказалось верным.
 */
public record TradeMetrics(UUID tradeId,
                           String ticker,
                           TradeType type,
                           LocalDate tradeDate,
                           BigDecimal price,
                           BigDecimal quantity,
                           Integer conviction,
                           TradeEmotion emotion,
                           String rationale,
                           Double pricePercentile90d,
                           Double priorReturn30d,
                           Double forwardReturn30d,
                           Double timingScore) {
}
