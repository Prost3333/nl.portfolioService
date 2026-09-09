package nlgrandtaskmanager.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

public class YahooResponse {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChartResponse(YahooChart chart) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChart(List<YahooResult> result) {
    }

    /** timestamp — секунды UTC для каждой свечи, параллельный массив к indicators.quote[0].close. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooResult(List<Long> timestamp, Indicators indicators) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Indicators(List<Quote> quote) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(List<BigDecimal> close) {
    }
}
