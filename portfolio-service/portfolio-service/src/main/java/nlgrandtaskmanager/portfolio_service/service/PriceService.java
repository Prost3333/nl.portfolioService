package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.client.YahooResponse;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final RestClient yahooRestClient;

    public BigDecimal getPrice(String ticker) {
        YahooResponse.YahooChartResponse response = yahooRestClient.get()
                .uri("/v8/finance/chart/{ticker}", ticker)
                .retrieve()
                .body(YahooResponse.YahooChartResponse.class);

        return response.chart().result().get(0).meta().regularMarketPrice();
    }
}
