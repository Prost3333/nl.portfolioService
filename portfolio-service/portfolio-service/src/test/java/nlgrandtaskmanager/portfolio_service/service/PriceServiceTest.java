package nlgrandtaskmanager.portfolio_service.service;

import nlgrandtaskmanager.portfolio_service.client.YahooResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private RestClient yahooRestClient;

    @InjectMocks
    private PriceService priceService;

    @Test
    void getPrice_returnsPrice_whenResponseIsValid() {
        YahooResponse.YahooChartResponse response = buildResponse(new BigDecimal("150.25"));
        givenRestClientReturns(response);

        BigDecimal price = priceService.getPrice("AAPL");

        assertThat(price).isEqualByComparingTo(new BigDecimal("150.25"));
    }

    @Test
    void getPrice_returnsNull_whenResponseBodyIsNull() {
        givenRestClientReturns(null);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_whenChartIsNull() {
        YahooResponse.YahooChartResponse response = new YahooResponse.YahooChartResponse(null);
        givenRestClientReturns(response);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_whenResultListIsNull() {
        YahooResponse.YahooChart chart = new YahooResponse.YahooChart(null);
        YahooResponse.YahooChartResponse response = new YahooResponse.YahooChartResponse(chart);
        givenRestClientReturns(response);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_whenResultListIsEmpty() {
        YahooResponse.YahooChart chart = new YahooResponse.YahooChart(List.of());
        YahooResponse.YahooChartResponse response = new YahooResponse.YahooChartResponse(chart);
        givenRestClientReturns(response);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_whenMetaIsNull() {
        YahooResponse.YahooResult result = new YahooResponse.YahooResult(null);
        YahooResponse.YahooChart chart = new YahooResponse.YahooChart(List.of(result));
        YahooResponse.YahooChartResponse response = new YahooResponse.YahooChartResponse(chart);
        givenRestClientReturns(response);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_whenMarketPriceIsNull() {
        YahooResponse.YahooChartResponse response = buildResponse(null);
        givenRestClientReturns(response);

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @Test
    void getPrice_returnsNull_onRestClientException() {
        givenRestClientThrows(new RestClientException("Connection refused"));

        assertThat(priceService.getPrice("AAPL")).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenRestClientReturns(YahooResponse.YahooChartResponse response) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(yahooRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class), any(Object.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(YahooResponse.YahooChartResponse.class)).thenReturn(response);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenRestClientThrows(RuntimeException ex) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(yahooRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class), any(Object.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(YahooResponse.YahooChartResponse.class)).thenThrow(ex);
    }

    private YahooResponse.YahooChartResponse buildResponse(BigDecimal price) {
        YahooResponse.YahooMeta meta = new YahooResponse.YahooMeta(price, "USD");
        YahooResponse.YahooResult result = new YahooResponse.YahooResult(meta);
        YahooResponse.YahooChart chart = new YahooResponse.YahooChart(List.of(result));
        return new YahooResponse.YahooChartResponse(chart);
    }
}
