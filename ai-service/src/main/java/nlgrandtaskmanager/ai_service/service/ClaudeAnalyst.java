package nlgrandtaskmanager.ai_service.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.ai_service.dto.BehaviorAnalysis;
import nlgrandtaskmanager.ai_service.dto.BehaviorMetrics;
import nlgrandtaskmanager.ai_service.dto.TradeMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/** Единственное место, где ai-service обращается к модели. */
@Service
@RequiredArgsConstructor
public class ClaudeAnalyst {

    /** Сколько последних сделок попадает в промпт: агрегаты считаются по всем. */
    private static final int MAX_TRADES_IN_PROMPT = 150;

    private static final String SYSTEM_PROMPT = """
            Ты анализируешь журнал сделок частного инвестора и описываешь его поведенческий профиль.

            Все числа уже посчитаны кодом. Используй их как есть, не пересчитывай и не выдумывай новые.

            Как читать метрики сделки:
            - pricePercentile90d: где цена сделки лежала в диапазоне закрытий за 90 дней до неё, 0..100.
              Покупки около 100 означают вход вдогонку за выросшей ценой.
            - priorReturn30d: движение цены за 30 дней до сделки, проценты.
              Продажи при сильно отрицательном значении означают выход на просадке.
            - forwardReturn30d: движение цены за 30 дней после сделки, проценты.
            - timingScore: результат решения через 30 дней. Для покупок совпадает с forwardReturn30d,
              для продаж берётся с обратным знаком. Положительный означает, что решение сработало.
            - conviction: заявленная уверенность от 1 до 5, emotion: состояние на момент сделки,
              rationale: причина сделки словами самого инвестора.

            Самое ценное в этих данных — расхождения между тем, что инвестор говорит, и тем, что делает:
            заявленная причина не совпадает с реальным поводом, высокая уверенность даёт результат хуже низкой,
            эмоция предсказывает исход лучше, чем обоснование.

            Пиши по-русски, обращайся на «ты». Опирайся только на то, что видно в данных: если выборка мала
            или разброс выглядит случайным, скажи об этом прямо вместо того, чтобы натягивать паттерн.
            Формулируй кратко и конкретно, ссылайся на тикеры и числа, а не на общие правила из учебника.
            """;

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.model:claude-opus-5}")
    private String model;

    public BehaviorAnalysis analyze(BehaviorMetrics metrics) {
        String payload = serialize(metrics);

        StructuredMessageCreateParams<BehaviorAnalysis> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(8000L)
                .system(SYSTEM_PROMPT)
                .outputConfig(BehaviorAnalysis.class)
                .addUserMessage("Журнал сделок и метрики:\n\n" + payload)
                .build();

        return anthropicClient.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Модель не вернула структурированный ответ"));
    }

    private String serialize(BehaviorMetrics metrics) {
        BehaviorMetrics payload = metrics;

        if (metrics.trades().size() > MAX_TRADES_IN_PROMPT) {
            List<TradeMetrics> recent = metrics.trades()
                    .subList(metrics.trades().size() - MAX_TRADES_IN_PROMPT, metrics.trades().size());
            payload = new BehaviorMetrics(
                    metrics.totalTrades(), metrics.buys(), metrics.sells(), metrics.evaluatedTrades(),
                    metrics.avgBuyPricePercentile(), metrics.avgSellPriorReturn30d(),
                    metrics.chaseBuyShare(), metrics.panicSellShare(),
                    metrics.avgTimingScore(), metrics.hitRate(), metrics.avgDaysBetweenTrades(),
                    metrics.byEmotion(), metrics.byConviction(), recent);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать метрики", e);
        }
    }
}
