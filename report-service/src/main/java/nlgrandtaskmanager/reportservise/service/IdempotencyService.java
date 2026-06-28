package nlgrandtaskmanager.reportservise.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.reportservise.model.ProcessedEvent;
import nlgrandtaskmanager.reportservise.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    @Transactional
    public boolean isAlreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Transactional
    public void markProcessed(UUID eventId) {
        repository.save(
                new ProcessedEvent(eventId, Instant.now())
        );
    }
}

