package nlgrandtaskmanager.ai_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.ai_service.dto.BehaviorAnalysis;
import nlgrandtaskmanager.ai_service.dto.BehaviorMetrics;
import nlgrandtaskmanager.ai_service.dto.BehaviorProfileResponse;
import nlgrandtaskmanager.ai_service.model.BehaviorProfile;
import nlgrandtaskmanager.ai_service.model.ProfileStatus;
import nlgrandtaskmanager.ai_service.repository.BehaviorProfileRepository;
import nlgrandtaskmanager.ai_service.repository.TradeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BehaviorAnalysisService {

    /** Через столько PENDING считается зависшим и перестаёт блокировать новый запуск. */
    private static final Duration STALE_AFTER = Duration.ofMinutes(15);

    private final TradeRecordRepository tradeRecordRepository;
    private final BehaviorProfileRepository profileRepository;
    private final BehaviorAnalysisRunner runner;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    /** Ниже этого числа сделок разговор о паттернах не имеет смысла. */
    @Value("${ai.min-trades:5}")
    private int minTrades;

    public BehaviorProfileResponse startAnalysis(UUID userId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ANTHROPIC_API_KEY не задан — анализ недоступен");
        }

        long trades = tradeRecordRepository.findByUserIdOrderByTradeDateAsc(userId).size();
        if (trades < minTrades) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Нужно минимум " + minTrades + " сделок в журнале, сейчас " + trades);
        }

        Instant recent = Instant.now().minus(STALE_AFTER);
        if (profileRepository.existsByUserIdAndStatusAndCreatedAtAfter(userId, ProfileStatus.PENDING, recent)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Анализ уже выполняется");
        }

        BehaviorProfile profile = profileRepository.save(BehaviorProfile.builder()
                .userId(userId)
                .status(ProfileStatus.PENDING)
                .tradesAnalyzed(0)
                .createdAt(Instant.now())
                .build());

        runner.run(profile.getId(), userId);

        return toResponse(profile);
    }

    public Optional<BehaviorProfileResponse> getLatest(UUID userId) {
        return profileRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).map(this::toResponse);
    }

    private BehaviorProfileResponse toResponse(BehaviorProfile profile) {
        return new BehaviorProfileResponse(
                profile.getId(),
                profile.getStatus(),
                profile.getTradesAnalyzed(),
                profile.getModelUsed(),
                profile.getCreatedAt(),
                profile.getCompletedAt(),
                read(profile.getMetricsJson(), BehaviorMetrics.class),
                read(profile.getAnalysisJson(), BehaviorAnalysis.class),
                profile.getErrorMessage());
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
