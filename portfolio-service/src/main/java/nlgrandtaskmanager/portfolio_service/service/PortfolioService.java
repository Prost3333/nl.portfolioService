package nlgrandtaskmanager.portfolio_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.*;
import nlgrandtaskmanager.portfolio_service.kafka.SnapshotEventProducer;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;
import nlgrandtaskmanager.portfolio_service.model.PortfolioSnapshot;
import nlgrandtaskmanager.portfolio_service.model.Trade;
import nlgrandtaskmanager.portfolio_service.repository.TradeRepository;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.repository.PortfolioSnapshotRepository;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.TreeSet;
import java.util.Set;
import java.util.NavigableMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final PriceService priceService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final SnapshotEventProducer snapshotEventProducer;
    private final TradeRepository tradeRepository;


    public PortfolioSummaryResponse getSummary(UUID userId) {
        List<PositionValue> values = positionRepository.findByUserId(userId).stream()
                .map(this::toPositionValue)
                .toList();

        BigDecimal totalValue = sumValues(values);
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return new PortfolioSummaryResponse(BigDecimal.ZERO, null, null, values);
        }

        List<PositionValue> positions = values.stream()
                .map(pv -> pv.priceAvailable() ? pv.withAllocation(allocationOf(pv, totalValue)) : pv)
                .toList();

        BigDecimal totalUnrealizedPL = sumUnrealizedPL(positions);
        BigDecimal totalCostBasis = sumCostBasis(positions);
        BigDecimal totalUnrealizedPLPercent = totalCostBasis.compareTo(BigDecimal.ZERO) > 0
                ? percentOf(totalUnrealizedPL, totalCostBasis)
                : null;

        return new PortfolioSummaryResponse(totalValue, totalUnrealizedPL, totalUnrealizedPLPercent, positions);
    }

    private PositionValue toPositionValue(Position position) {
        BigDecimal price = priceService.getPrice(position.getTicker());
        if (price == null) {
            return new PositionValue(position.getTicker(), position.getName(), position.getQuantity(),
                    null, null, BigDecimal.ZERO, null, null, null, false);
        }

        BigDecimal value = position.getQuantity()
                .multiply(price)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averagePrice = position.getAveragePrice();
        BigDecimal unrealizedPL = null;
        BigDecimal unrealizedPLPercent = null;
        if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costBasis = averagePrice.multiply(position.getQuantity());
            unrealizedPL = value.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);
            // у полностью проданной бумаги количество нулевое, значит и вложено в неё ноль:
            // процент от нуля не определён, percentOf на таком costBasis падает с ArithmeticException
            unrealizedPLPercent = costBasis.signum() > 0 ? percentOf(unrealizedPL, costBasis) : null;
        }

        return new PositionValue(position.getTicker(), position.getName(), position.getQuantity(),
                price, value, BigDecimal.ZERO, averagePrice, unrealizedPL, unrealizedPLPercent, true);
    }

    private static BigDecimal sumValues(List<PositionValue> positions) {
        return positions.stream()
                .filter(PositionValue::priceAvailable)
                .map(PositionValue::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumUnrealizedPL(List<PositionValue> positions) {
        return positions.stream()
                .map(PositionValue::unrealizedPL)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumCostBasis(List<PositionValue> positions) {
        return positions.stream()
                .filter(pv -> pv.priceAvailable() && pv.averagePrice() != null)
                .map(pv -> pv.averagePrice().multiply(pv.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal allocationOf(PositionValue position, BigDecimal totalValue) {
        return position.value()
                .divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal base) {
        return amount
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void saveSnapshot(UUID userId) {
        LocalDate today = LocalDate.now();

        if (snapshotRepository.existsByUserIdAndSnapshotDate(userId, today)) {
            return;
        }
        BigDecimal totalValue = getSummary(userId).totalValue();

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .userId(userId)
                .totalValue(totalValue)
                .snapshotDate(today)
                .build();

        snapshotRepository.save(snapshot);
        snapshotEventProducer.publish(new SnapshotCreatedEvent(snapshot.getId()
                , snapshot.getUserId(), today, snapshot.getTotalValue()));


    }

    /**
     * Восстанавливает историю стоимости портфеля задним числом, по журналу сделок.
     * {@link #saveSnapshot} умеет писать только сегодняшний день, поэтому импортированные
     * сделки сами по себе графика не дают — до первого запуска планировщика история пуста.
     * <p>
     * На каждый торговый день состав портфеля берётся из сделок, цена — из дневных
     * закрытий Yahoo. Уже существующие снимки не трогаются, так что метод можно
     * вызывать повторно после добавления новых сделок.
     *
     * @return сколько снимков создано
     */
    @Transactional
    public int backfillHistory(UUID userId) {
        List<Trade> trades = tradeRepository.findByUserIdOrderByTradeDateDesc(userId);
        if (trades.isEmpty()) {
            return 0;
        }

        LocalDate from = trades.stream().map(Trade::getTradeDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = LocalDate.now();

        Map<String, NavigableMap<LocalDate, BigDecimal>> closes = new HashMap<>();
        Set<LocalDate> tradingDays = new TreeSet<>();
        for (String ticker : trades.stream().map(Trade::getTicker).collect(Collectors.toSet())) {
            NavigableMap<LocalDate, BigDecimal> series = priceService.getDailyCloses(ticker, from, to);
            closes.put(ticker, series);
            tradingDays.addAll(series.keySet());
        }
        if (tradingDays.isEmpty()) {
            return 0;
        }

        Map<LocalDate, List<Trade>> tradesByDate = trades.stream()
                .collect(Collectors.groupingBy(Trade::getTradeDate));

        Set<LocalDate> alreadyStored = snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId).stream()
                .map(PortfolioSnapshot::getSnapshotDate)
                .collect(Collectors.toSet());

        Map<String, BigDecimal> holdings = new LinkedHashMap<>();
        List<PortfolioSnapshot> snapshots = new ArrayList<>();

        for (LocalDate day : tradingDays) {
            for (Trade trade : tradesByDate.getOrDefault(day, List.of())) {
                BigDecimal signed = trade.getType() == TradeType.BUY
                        ? trade.getQuantity()
                        : trade.getQuantity().negate();
                holdings.merge(trade.getTicker(), signed, BigDecimal::add);
            }

            if (alreadyStored.contains(day) || holdings.isEmpty()) {
                continue;
            }

            BigDecimal totalValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> holding : holdings.entrySet()) {
                if (holding.getValue().signum() == 0) {
                    continue;
                }
                Map.Entry<LocalDate, BigDecimal> close = closes.get(holding.getKey()).floorEntry(day);
                if (close == null) {
                    continue;
                }
                totalValue = totalValue.add(holding.getValue().multiply(close.getValue()));
            }

            if (totalValue.signum() == 0) {
                continue;
            }

            snapshots.add(PortfolioSnapshot.builder()
                    .userId(userId)
                    .snapshotDate(day)
                    .totalValue(totalValue.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        List<PortfolioSnapshot> saved = snapshotRepository.saveAll(snapshots);
        saved.forEach(snapshot -> snapshotEventProducer.publish(new SnapshotCreatedEvent(
                snapshot.getId(), userId, snapshot.getSnapshotDate(), snapshot.getTotalValue())));

        return saved.size();
    }

    public List<SnapshotResponse> getHistory(UUID userId, String period) {
        LocalDate fromDate = switch (period) {
            case "week" -> LocalDate.now().minusWeeks(1);
            case "month" -> LocalDate.now().minusMonths(1);
            case "6months" -> LocalDate.now().minusMonths(6);
            case "year" -> LocalDate.now().minusYears(1);
            default -> LocalDate.of(1970, 1, 1);
        };

        return snapshotRepository
                .findByUserIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(userId, fromDate)
                .stream()
                .map(s -> new SnapshotResponse(s.getSnapshotDate(), s.getTotalValue()))
                .toList();
    }

    public List<PerformanceItem> getPerformance(UUID userId, String period) {
        String range = getRange(period);

        return positionRepository.findByUserId(userId).stream()
                .map(position -> {
                    BigDecimal change = priceService.getPriceChangePercent(position.getTicker(), range);
                    return new PerformanceItem(
                            position.getTicker(),
                            position.getName(),
                            change,
                            change != null
                    );
                })
                .toList();
    }

    public BigDecimal getPercentChanges(UUID userId, String period) {

        List<SnapshotResponse> list = getHistory(userId, period);
        if (list.size() < 2) {
            return null;
        }

        SnapshotResponse first = list.get(0);
        SnapshotResponse last = list.get(list.size() - 1);

        if (first.totalValue() == null
                || last.totalValue() == null
                || first.totalValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return last.totalValue()
                .subtract(first.totalValue())
                .divide(first.totalValue(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

    }

    public String getRange(String period) {
        return switch (period) {
            case "week" -> "5d";
            case "month" -> "1mo";
            case "6months" -> "6mo";
            case "year" -> "1y";
            default -> "1mo";
        };
    }

}
