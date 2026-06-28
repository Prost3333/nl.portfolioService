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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final PriceService priceService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private  final SnapshotEventProducer snapshotEventProducer;


    public PortfolioSummaryResponse getSummary(UUID userId) {
        List<Position> positions = positionRepository.findByUserId(userId);

        List<PositionValue> rawValues = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Position position : positions) {
            BigDecimal price = priceService.getPrice(position.getTicker());

            if (price == null) {
                rawValues.add(new PositionValue(
                        position.getTicker(),
                        position.getName(),
                        position.getQuantity(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        false
                ));
                continue;
            }

            BigDecimal value = position.getQuantity()
                    .multiply(price)
                    .setScale(2, RoundingMode.HALF_UP);
            rawValues.add(new PositionValue(
                    position.getTicker(),
                    position.getName(),
                    position.getQuantity(),
                    price,
                    value,
                    BigDecimal.ZERO,
                    true
            ));
            total = total.add(value);
        }

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new PortfolioSummaryResponse(BigDecimal.ZERO, rawValues);
        }


        List<PositionValue> result = new ArrayList<>();
        for (PositionValue pv : rawValues) {
            if (!pv.priceAvailable()) {
                result.add(pv);
                continue;
            }
            BigDecimal percent = pv.value()
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.add(new PositionValue(
                    pv.ticker(), pv.name(), pv.quantity(),
                    pv.price(), pv.value(), percent, true
            ));
        }

        return new PortfolioSummaryResponse(total, result);
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
                ,snapshot.getUserId(),today,snapshot.getTotalValue()));


    }

    public List<SnapshotResponse> getHistory(UUID userId, String period) {
        LocalDate fromDate = switch (period) {
            case "week"    -> LocalDate.now().minusWeeks(1);
            case "month"   -> LocalDate.now().minusMonths(1);
            case "6months" -> LocalDate.now().minusMonths(6);
            case "year"    -> LocalDate.now().minusYears(1);
            default        -> LocalDate.of(1970, 1, 1);
        };

        return snapshotRepository
                .findByUserIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(userId, fromDate)
                .stream()
                .map(s -> new SnapshotResponse(s.getSnapshotDate(), s.getTotalValue()))
                .toList();
    }

    public List<PerformanceItem> getPerformance(UUID userId, String period) {
        String range=getRange(period);

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

    public BigDecimal getPercentChanges (UUID userId,String period){

        List<SnapshotResponse> list=getHistory(userId,period);
        if (list.size()<2){
            return null;
        }

        SnapshotResponse first=list.get(0);
        SnapshotResponse last=list.get(list.size()-1);

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

    public  String getRange(String period){
        return switch (period) {
            case "week"    -> "5d";
            case "month"   -> "1mo";
            case "6months" -> "6mo";
            case "year"    -> "1y";
            default        -> "1mo";
        };
    }

}
