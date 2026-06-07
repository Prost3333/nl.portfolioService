package nlgrandtaskmanager.portfolio_service.service;

import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.dto.PositionValue;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final PriceService priceService;

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

            BigDecimal value = position.getQuantity().multiply(price);
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
}
