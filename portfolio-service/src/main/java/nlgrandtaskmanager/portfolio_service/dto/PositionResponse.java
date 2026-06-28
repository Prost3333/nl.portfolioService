package nlgrandtaskmanager.portfolio_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
        UUID id,
        String ticker,
        String name,
        BigDecimal quantity,
        Instant createdAt
) {}
