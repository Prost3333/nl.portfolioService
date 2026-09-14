package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.client.YahooResponse;
import nlgrandtaskmanager.portfolio_service.dto.TickerInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

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

    public TickerInfo getQuote(String ticker){
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


            return new TickerInfo(meta.regularMarketPrice(), meta.longName());

        } catch (RestClientException e) {
            return null;
        }
    }

    /**
     * Дневные цены закрытия за период — нужны, чтобы восстановить стоимость портфеля
     * задним числом. Ключ — торговый день, выходных и праздников в карте просто нет.
     * Пустая карта означает, что Yahoo не отдал историю по этому тикеру.
     */
    @Cacheable(value = "priceHistory", unless = "#result.isEmpty()")
    public NavigableMap<LocalDate, BigDecimal> getDailyCloses(String ticker, LocalDate from, LocalDate to) {
        NavigableMap<LocalDate, BigDecimal> series = new TreeMap<>();
        try {
            YahooResponse.YahooChartResponse response = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?period1={from}&period2={to}&interval=1d",
                            ticker,
                            from.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
                            to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond())
                    .retrieve()
                    .body(YahooResponse.YahooChartResponse.class);

            if (response == null
                    || response.chart() == null
                    || response.chart().result() == null
                    || response.chart().result().isEmpty()) {
                return series;
            }

            var result = response.chart().result().get(0);

            if (result.timestamp() == null
                    || result.indicators() == null
                    || result.indicators().quote() == null
                    || result.indicators().quote().isEmpty()) {
                return series;
            }

            List<BigDecimal> closes = result.indicators().quote().get(0).close();
            List<Long> timestamps = result.timestamp();

            if (closes == null) {
                return series;
            }

            for (int i = 0; i < Math.min(timestamps.size(), closes.size()); i++) {
                BigDecimal close = closes.get(i);
                if (close == null) {
                    continue;
                }
                series.put(Instant.ofEpochSecond(timestamps.get(i)).atZone(ZoneOffset.UTC).toLocalDate(), close);
            }

            return series;

        } catch (RestClientException e) {
            return series;
        }
    }
}

