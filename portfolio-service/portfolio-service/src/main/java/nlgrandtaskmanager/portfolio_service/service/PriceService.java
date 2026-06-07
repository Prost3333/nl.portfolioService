package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.client.YahooResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final RestClient yahooRestClient;

    @Cacheable(value = "prices", unless = "#result == null")
    public BigDecimal getPrice(String ticker) {
        try {
            YahooResponse.YahooChartResponse response = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}", ticker)
                    .retrieve()
                    .body(YahooResponse.YahooChartResponse.class);

            if (response == null
                    || response.chart() == null
                    || response.chart().result() == null
                    || response.chart().result().isEmpty()) {
                return null;
            }

            var meta = response.chart().result().get(0).meta();
            if (meta == null || meta.regularMarketPrice() == null) {
                return null;
            }

            return meta.regularMarketPrice();

        } catch (RestClientException e) {
            return null;
        }
    }
}
