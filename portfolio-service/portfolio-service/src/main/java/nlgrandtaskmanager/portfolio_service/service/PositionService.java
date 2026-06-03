package nlgrandtaskmanager.portfolio_service.service;


import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.dto.CreatePositionRequest;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionService {
    private  final PositionRepository positionRepository;

    public PositionResponse create (UUID userId, CreatePositionRequest request){
        Position position=Position.builder()
                .userId(userId)
                .ticker(request.ticker())
                .name(request.name())
                .quantity(request.quantity())
                .createdAt(Instant.now())
                .build();

        Position saved=positionRepository.save(position);
        return  toResponse(saved);
    }

    public List<PositionResponse> getPositions(UUID userId) {
        return positionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

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
