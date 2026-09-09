package nlgrandtaskmanager.ai_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nlgrandtaskmanager.ai_service.dto.YahooResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Дневные закрытия по тикеру из Yahoo Finance — рыночный контекст вокруг каждой сделки. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final RestClient yahooRestClient;

    /**
     * Пять лет дневных свечей одним запросом: журнал сделок обычно укладывается в этот срок,
     * а кэш избавляет от повторных обращений по тому же тикеру внутри одного анализа.
     *
     * @return закрытия по датам; пустая карта, если Yahoo не отдал данные
     */
    @Cacheable("priceHistory")
    public NavigableMap<LocalDate, BigDecimal> getDailyCloses(String ticker) {
        try {
            YahooResponse.YahooChartResponse response = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?range=5y&interval=1d", ticker)
                    .retrieve()
                    .body(YahooResponse.YahooChartResponse.class);

            if (response == null
                    || response.chart() == null
                    || response.chart().result() == null
                    || response.chart().result().isEmpty()) {
                return Collections.emptyNavigableMap();
            }

            YahooResponse.YahooResult result = response.chart().result().get(0);

            if (result.timestamp() == null
                    || result.indicators() == null
                    || result.indicators().quote() == null
                    || result.indicators().quote().isEmpty()) {
                return Collections.emptyNavigableMap();
            }

            List<Long> timestamps = result.timestamp();
            List<BigDecimal> closes = result.indicators().quote().get(0).close();

            if (closes == null) {
                return Collections.emptyNavigableMap();
            }

            NavigableMap<LocalDate, BigDecimal> history = new TreeMap<>();
            int size = Math.min(timestamps.size(), closes.size());

            for (int i = 0; i < size; i++) {
                BigDecimal close = closes.get(i);
                if (close == null || timestamps.get(i) == null) {
                    continue;
                }
                LocalDate date = Instant.ofEpochSecond(timestamps.get(i))
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();
                history.put(date, close);
            }

            return history;

        } catch (RestClientException e) {
            log.warn("Не удалось получить историю котировок {}: {}", ticker, e.getMessage());
            return Collections.emptyNavigableMap();
        }
    }
}
