package nlgrandtaskmanager.portfolio_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class YahooResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChartResponse(YahooChart chart) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChart(List<YahooResult> result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooResult(YahooMeta meta) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooMeta(
            @JsonProperty("regularMarketPrice") BigDecimal regularMarketPrice,
            String currency
    ) {}
}
