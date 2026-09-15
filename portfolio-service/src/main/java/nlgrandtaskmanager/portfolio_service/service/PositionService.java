package nlgrandtaskmanager.portfolio_service.service;


import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository positionRepository;

    public List<PositionResponse> getPositions(UUID userId) {
        return positionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));

        if (!position.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // позиция выводится из журнала: удалить строку, не тронув сделки, бессмысленно —
        // rebuildPositions соберёт её обратно. Убирать из списка можно только закрытую бумагу
        if (position.getQuantity().signum() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Position " + position.getTicker() + " still holds "
                            + position.getQuantity().toPlainString()
                            + " shares: sell them before removing it");
        }
        positionRepository.delete(position);



    }

    private PositionResponse toResponse(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getTicker(),
                position.getName(),
                position.getQuantity(),
                position.getCreatedAt()
        );
    }
}
