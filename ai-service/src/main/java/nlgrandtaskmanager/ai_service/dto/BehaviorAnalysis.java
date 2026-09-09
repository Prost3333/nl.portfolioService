package nlgrandtaskmanager.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Интерпретация метрик и формулировок из журнала. Схема этого класса уходит в structured outputs. */
@JsonClassDescription("Поведенческий профиль инвестора по его журналу сделок")
public record BehaviorAnalysis(

        @JsonPropertyDescription("Портрет инвестора в 2-3 предложениях: как он принимает решения")
        String summary,

        @JsonPropertyDescription("Устойчивые поведенческие паттерны, подтверждённые метриками")
        List<BehaviorPattern> patterns,

        @JsonPropertyDescription("Что инвестор делает хорошо, 1-3 пункта")
        List<String> strengths,

        @JsonPropertyDescription("Конкретные рекомендации по изменению процесса, 2-4 пункта")
        List<String> recommendations) {

    public record BehaviorPattern(

            @JsonPropertyDescription("Короткое название паттерна")
            String title,

            @JsonPropertyDescription("Насколько паттерн вредит результату: LOW, MEDIUM или HIGH")
            String severity,

            @JsonPropertyDescription("В чём заключается паттерн и как он влияет на результат")
            String description,

            @JsonPropertyDescription("Конкретные сделки и числа, на которых основан вывод")
            String evidence) {
    }
}
