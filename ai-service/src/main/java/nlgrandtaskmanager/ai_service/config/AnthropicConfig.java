package nlgrandtaskmanager.ai_service.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AnthropicConfig {

    /**
     * Клиент создаётся даже без ключа, чтобы сервис поднимался и отдавал остальные эндпоинты.
     * Отсутствие ключа проверяется в BehaviorAnalysisService перед вызовом модели.
     */
    @Bean
    public AnthropicClient anthropicClient(@Value("${anthropic.api-key:}") String apiKey) {
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey == null || apiKey.isBlank() ? "not-configured" : apiKey)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
