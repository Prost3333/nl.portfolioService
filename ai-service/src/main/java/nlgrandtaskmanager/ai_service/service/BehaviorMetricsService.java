package nlgrandtaskmanager.ai_service.service;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.ai_service.dto.BehaviorMetrics;
import nlgrandtaskmanager.ai_service.dto.TradeMetrics;
import nlgrandtaskmanager.ai_service.model.TradeEmotion;
import nlgrandtaskmanager.ai_service.model.TradeRecord;
import nlgrandtaskmanager.ai_service.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Считает рыночный контекст сделок. Вся арифметика живёт здесь, а не в промпте:
 * модель получает уже готовые числа и только интерпретирует их.
 */
@Service
@RequiredArgsConstructor
public class BehaviorMetricsService {

    /** Окно, относительно которого оценивается «дорого ли куплено». */
    private static final int LOOKBACK_DAYS = 90;

    /** Горизонт, на котором проверяется, оказалось ли решение верным. */
    private static final int WINDOW_DAYS = 30;

    /** Покупка выше этого перцентиля 90-дневного диапазона считается покупкой вдогонку. */
    private static final double CHASE_PERCENTILE = 80.0;

    /** Продажа после такого падения за 30 дней считается продажей на просадке. */
    private static final double PANIC_DROP_PERCENT = -10.0;

    /** Насколько далеко от нужной даты допустимо брать ближайшую котировку. */
    private static final int MAX_LOOKUP_GAP_DAYS = 10;

    private final PriceHistoryService priceHistoryService;

    public BehaviorMetrics compute(List<TradeRecord> trades) {
        Map<String, NavigableMap<LocalDate, BigDecimal>> histories = new HashMap<>();

        List<TradeMetrics> enriched = trades.stream()
                .sorted(Comparator.comparing(TradeRecord::getTradeDate))
                .map(trade -> toMetrics(trade, histories.computeIfAbsent(
                        trade.getTicker(), priceHistoryService::getDailyCloses)))
                .toList();

        List<TradeMetrics> buys = enriched.stream()
                .filter(t -> t.type() == TradeType.BUY)
                .toList();
        List<TradeMetrics> sells = enriched.stream()
                .filter(t -> t.type() == TradeType.SELL)
                .toList();
        List<TradeMetrics> evaluated = enriched.stream()
                .filter(t -> t.timingScore() != null)
                .toList();

        return new BehaviorMetrics(
                enriched.size(),
                buys.size(),
                sells.size(),
                evaluated.size(),
                average(buys, TradeMetrics::pricePercentile90d),
                average(sells, TradeMetrics::priorReturn30d),
                share(buys, t -> t.pricePercentile90d() != null && t.pricePercentile90d() >= CHASE_PERCENTILE),
                share(sells, t -> t.priorReturn30d() != null && t.priorReturn30d() <= PANIC_DROP_PERCENT),
                average(evaluated, TradeMetrics::timingScore),
                hitRate(evaluated),
                averageGapDays(enriched),
                emotionStats(enriched),
                convictionStats(enriched),
                enriched);
    }

    private TradeMetrics toMetrics(TradeRecord trade, NavigableMap<LocalDate, BigDecimal> history) {
        LocalDate date = trade.getTradeDate();
        double price = trade.getPrice().doubleValue();

        Double percentile = percentileInRange(history, date, price);
        Double priorReturn = returnBetween(closeOnOrBefore(history, date.minusDays(WINDOW_DAYS)), price);
        Double forwardReturn = returnBetween(price, closeOnOrAfter(history, date.plusDays(WINDOW_DAYS)));

        Double timingScore = null;
        if (forwardReturn != null) {
            timingScore = trade.getType() == TradeType.BUY ? forwardReturn : -forwardReturn;
        }

        return new TradeMetrics(
                trade.getTradeId(),
                trade.getTicker(),
                trade.getType(),
                date,
                trade.getPrice(),
                trade.getQuantity(),
                trade.getConviction(),
                trade.getEmotion(),
                trade.getRationale(),
                percentile,
                priorReturn,
                forwardReturn,
                round(timingScore));
    }

    /** Где цена сделки лежит в диапазоне закрытий за LOOKBACK_DAYS до неё, 0..100. */
    private Double percentileInRange(NavigableMap<LocalDate, BigDecimal> history, LocalDate date, double price) {
        NavigableMap<LocalDate, BigDecimal> window =
                history.subMap(date.minusDays(LOOKBACK_DAYS), true, date, false);

        if (window.isEmpty()) {
            return null;
        }

        OptionalDouble min = window.values().stream().mapToDouble(BigDecimal::doubleValue).min();
        OptionalDouble max = window.values().stream().mapToDouble(BigDecimal::doubleValue).max();

        if (min.isEmpty() || max.isEmpty() || max.getAsDouble() == min.getAsDouble()) {
            return null;
        }

        double raw = (price - min.getAsDouble()) / (max.getAsDouble() - min.getAsDouble()) * 100.0;
        return round(Math.max(0.0, Math.min(100.0, raw)));
    }

    private Double closeOnOrBefore(NavigableMap<LocalDate, BigDecimal> history, LocalDate date) {
        var entry = history.floorEntry(date);
        if (entry == null || ChronoUnit.DAYS.between(entry.getKey(), date) > MAX_LOOKUP_GAP_DAYS) {
            return null;
        }
        return entry.getValue().doubleValue();
    }

    /**
     * Ближайшее закрытие не раньше указанной даты. Допуск нужен, чтобы после выходных
     * или паузы в торгах не подставить котировку, отстоящую на месяцы: такой «результат
     * через 30 дней» измерял бы совсем другой горизонт.
     */
    private Double closeOnOrAfter(NavigableMap<LocalDate, BigDecimal> history, LocalDate date) {
        var entry = history.ceilingEntry(date);
        if (entry == null || ChronoUnit.DAYS.between(date, entry.getKey()) > MAX_LOOKUP_GAP_DAYS) {
            return null;
        }
        return entry.getValue().doubleValue();
    }

    private Double returnBetween(Double from, Double to) {
        if (from == null || to == null || from == 0.0) {
            return null;
        }
        return round((to / from - 1.0) * 100.0);
    }

    private Double average(List<TradeMetrics> trades, java.util.function.Function<TradeMetrics, Double> field) {
        OptionalDouble avg = trades.stream()
                .map(field)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return avg.isPresent() ? round(avg.getAsDouble()) : null;
    }

    private Double share(List<TradeMetrics> trades, java.util.function.Predicate<TradeMetrics> predicate) {
        if (trades.isEmpty()) {
            return null;
        }
        long matching = trades.stream().filter(predicate).count();
        return round(matching * 100.0 / trades.size());
    }

    private Double hitRate(List<TradeMetrics> evaluated) {
        if (evaluated.isEmpty()) {
            return null;
        }
        long positive = evaluated.stream().filter(t -> t.timingScore() > 0).count();
        return round(positive * 100.0 / evaluated.size());
    }

    private Double averageGapDays(List<TradeMetrics> trades) {
        if (trades.size() < 2) {
            return null;
        }
        List<Double> gaps = new ArrayList<>();
        for (int i = 1; i < trades.size(); i++) {
            gaps.add((double) ChronoUnit.DAYS.between(trades.get(i - 1).tradeDate(), trades.get(i).tradeDate()));
        }
        return round(gaps.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private List<BehaviorMetrics.EmotionStat> emotionStats(List<TradeMetrics> trades) {
        Map<TradeEmotion, List<TradeMetrics>> grouped = trades.stream()
                .filter(t -> t.emotion() != null)
                .collect(Collectors.groupingBy(TradeMetrics::emotion));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<TradeMetrics> evaluated = withScore(entry.getValue());
                    return new BehaviorMetrics.EmotionStat(
                            entry.getKey(),
                            entry.getValue().size(),
                            average(evaluated, TradeMetrics::timingScore),
                            hitRate(evaluated));
                })
                .sorted(Comparator.comparing(s -> s.emotion().name()))
                .toList();
    }

    private List<BehaviorMetrics.ConvictionStat> convictionStats(List<TradeMetrics> trades) {
        Map<Integer, List<TradeMetrics>> grouped = trades.stream()
                .filter(t -> t.conviction() != null)
                .collect(Collectors.groupingBy(TradeMetrics::conviction));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<TradeMetrics> evaluated = withScore(entry.getValue());
                    return new BehaviorMetrics.ConvictionStat(
                            entry.getKey(),
                            entry.getValue().size(),
                            average(evaluated, TradeMetrics::timingScore),
                            hitRate(evaluated));
                })
                .sorted(Comparator.comparingInt(BehaviorMetrics.ConvictionStat::conviction))
                .toList();
    }

    private List<TradeMetrics> withScore(List<TradeMetrics> trades) {
        return trades.stream().filter(t -> t.timingScore() != null).toList();
    }

    private Double round(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
