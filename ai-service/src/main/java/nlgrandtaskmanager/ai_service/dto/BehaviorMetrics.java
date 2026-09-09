package nlgrandtaskmanager.ai_service.dto;

import nlgrandtaskmanager.ai_service.model.TradeEmotion;

import java.util.List;

/**
 * Агрегаты по всему журналу сделок пользователя.
 *
 * @param evaluatedTrades      сделки, по которым уже прошло 30 дней и известен результат
 * @param avgBuyPricePercentile средний перцентиль входа по покупкам
 * @param chaseBuyShare        доля покупок в верхних 20% 90-дневного диапазона
 * @param panicSellShare       доля продаж после падения цены более чем на 10% за 30 дней
 * @param hitRate              доля сделок с положительным timingScore
 */
public record BehaviorMetrics(int totalTrades,
                              int buys,
                              int sells,
                              int evaluatedTrades,
                              Double avgBuyPricePercentile,
                              Double avgSellPriorReturn30d,
                              Double chaseBuyShare,
                              Double panicSellShare,
                              Double avgTimingScore,
                              Double hitRate,
                              Double avgDaysBetweenTrades,
                              List<EmotionStat> byEmotion,
                              List<ConvictionStat> byConviction,
                              List<TradeMetrics> trades) {

    public record EmotionStat(TradeEmotion emotion, int count, Double avgTimingScore, Double hitRate) {
    }

    public record ConvictionStat(int conviction, int count, Double avgTimingScore, Double hitRate) {
    }
}
