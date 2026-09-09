package nlgrandtaskmanager.ai_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nlgrandtaskmanager.ai_service.dto.BehaviorAnalysis;
import nlgrandtaskmanager.ai_service.dto.BehaviorMetrics;
import nlgrandtaskmanager.ai_service.model.BehaviorProfile;
import nlgrandtaskmanager.ai_service.model.ProfileStatus;
import nlgrandtaskmanager.ai_service.model.TradeRecord;
import nlgrandtaskmanager.ai_service.repository.BehaviorProfileRepository;
import nlgrandtaskmanager.ai_service.repository.TradeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Генерация профиля идёт в фоне: обращение к модели занимает десятки секунд,
 * а HTTP-запрос от фронта столько ждать не должен.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorAnalysisRunner {

    private final TradeRecordRepository tradeRecordRepository;
    private final BehaviorProfileRepository profileRepository;
    private final BehaviorMetricsService metricsService;
    private final ClaudeAnalyst claudeAnalyst;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.model:claude-opus-5}")
    private String model;

    @Async
    public void run(UUID profileId, UUID userId) {
        BehaviorProfile profile = profileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            log.warn("Профиль {} исчез до начала анализа", profileId);
            return;
        }

        try {
            List<TradeRecord> trades = tradeRecordRepository.findByUserIdOrderByTradeDateAsc(userId);
            BehaviorMetrics metrics = metricsService.compute(trades);

            profile.setMetricsJson(objectMapper.writeValueAsString(metrics));
            profile.setTradesAnalyzed(metrics.totalTrades());

            BehaviorAnalysis analysis = claudeAnalyst.analyze(metrics);

            profile.setAnalysisJson(objectMapper.writeValueAsString(analysis));
            profile.setModelUsed(model);
            profile.setStatus(ProfileStatus.READY);
        } catch (Exception e) {
            log.error("Анализ профиля {} завершился ошибкой", profileId, e);
            profile.setStatus(ProfileStatus.FAILED);
            profile.setErrorMessage(truncate(e.getMessage()));
        } finally {
            profile.setCompletedAt(Instant.now());
            profileRepository.save(profile);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Неизвестная ошибка";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
