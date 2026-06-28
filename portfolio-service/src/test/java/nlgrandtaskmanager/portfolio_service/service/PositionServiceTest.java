package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.dto.CreatePositionRequest;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.model.Position;
import nlgrandtaskmanager.portfolio_service.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void create_savesPositionWithCorrectFields() {
        CreatePositionRequest request = new CreatePositionRequest("AAPL", "Apple Inc", BigDecimal.TEN);
        Position saved = buildPosition(UUID.randomUUID(), userId, "AAPL", "Apple Inc", BigDecimal.TEN);

        when(positionRepository.save(any(Position.class))).thenReturn(saved);

        PositionResponse response = positionService.create(userId, request);

        assertThat(response.ticker()).isEqualTo("AAPL");
        assertThat(response.name()).isEqualTo("Apple Inc");
        assertThat(response.quantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(response.id()).isEqualTo(saved.getId());

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(captor.capture());
        Position captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getTicker()).isEqualTo("AAPL");
        assertThat(captured.getName()).isEqualTo("Apple Inc");
        assertThat(captured.getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    @Test
    void getPositions_returnsAllPositionsForUser() {
        Position p1 = buildPosition(UUID.randomUUID(), userId, "AAPL", "Apple", new BigDecimal("5"));
        Position p2 = buildPosition(UUID.randomUUID(), userId, "GOOG", "Google", new BigDecimal("3"));

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(p1, p2));

        List<PositionResponse> result = positionService.getPositions(userId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PositionResponse::ticker).containsExactly("AAPL", "GOOG");
    }

    @Test
    void getPositions_returnsEmptyList_whenUserHasNoPositions() {
        when(positionRepository.findByUserId(userId)).thenReturn(List.of());

        List<PositionResponse> result = positionService.getPositions(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void delete_removesPosition_whenUserIsOwner() {
        UUID positionId = UUID.randomUUID();
        Position position = buildPosition(positionId, userId, "AAPL", "Apple", BigDecimal.ONE);

        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));

        positionService.delete(userId, positionId);

        verify(positionRepository).delete(position);
    }

    @Test
    void delete_throwsNotFound_whenPositionDoesNotExist() {
        UUID positionId = UUID.randomUUID();
        when(positionRepository.findById(positionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionService.delete(userId, positionId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(positionRepository, never()).delete(any());
    }

    @Test
    void delete_throwsForbidden_whenUserIsNotOwner() {
        UUID positionId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Position position = buildPosition(positionId, otherUserId, "AAPL", "Apple", BigDecimal.ONE);

        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> positionService.delete(userId, positionId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(positionRepository, never()).delete(any());
    }

    private Position buildPosition(UUID id, UUID ownerId, String ticker, String name, BigDecimal quantity) {
        return Position.builder()
                .id(id).userId(ownerId)
                .ticker(ticker).name(name).quantity(quantity)
                .createdAt(Instant.now())
                .build();
    }
}
