package nlgrandtaskmanager.portfolio_service.service;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.*;
import nlgrandtaskmanager.portfolio_service.kafka.SnapshotEventProducer;
import nlgrandtaskmanager.portfolio_service.model.PortfolioSnapshot;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.repository.PortfolioSnapshotRepository;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final PriceService priceService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final SnapshotEventProducer snapshotEventProducer;


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
            unrealizedPLPercent = percentOf(unrealizedPL, costBasis);
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
