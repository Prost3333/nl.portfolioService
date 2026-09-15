package nlgrandtaskmanager.portfolio_service.service;


import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import nlgrandtaskmanager.portfolio_service.repository.TradeRepository;
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

    private final TradeRepository tradeRepository;


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
