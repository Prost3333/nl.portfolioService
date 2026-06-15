package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.client.YahooResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    @Cacheable(value = "priceChanges", unless = "#result == null")
    public BigDecimal getPriceChangePercent(String ticker, String range) {

        try {

            YahooResponse.YahooChartResponse response = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?range={range}&interval=1d",
                            ticker,
                            range)
                    .retrieve()
                    .body(YahooResponse.YahooChartResponse.class);

            List<BigDecimal> closes = extractCloses(response);

            if (closes == null) {
                return null;
            }

            BigDecimal first = closes.get(0);
            BigDecimal last = closes.get(closes.size() - 1);

            if (first == null
                    || last == null
                    || first.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }

            return last.subtract(first)
                    .divide(first, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

        } catch (RestClientException e) {
            return null;
        }
    }


    private List<BigDecimal> extractCloses (YahooResponse.YahooChartResponse response){
        if (response == null
                || response.chart() == null
                || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            return null;
        }

        var result = response.chart().result().get(0);

        if (result.indicators() == null
                || result.indicators().quote() == null
                || result.indicators().quote().isEmpty()) {
            return null;
        }

        List<BigDecimal> closes = result.indicators().quote().get(0).close();

        if (closes == null || closes.size() < 2) {
            return null;
        }

        return closes;
    }
    }
