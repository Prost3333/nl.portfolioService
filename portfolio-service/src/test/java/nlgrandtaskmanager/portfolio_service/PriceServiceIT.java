package nlgrandtaskmanager.portfolio_service;

import nlgrandtaskmanager.portfolio_service.service.PriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PriceServiceIT {

    @Autowired
    private PriceService priceService;

    @Test
    void getPrice_returnsRealPriceFromYahoo() {
        BigDecimal price = priceService.getPrice("NOV.DE");

        System.out.println("Цена NOV.DE: " + price);

        assertThat(price).isNotNull();
        assertThat(price).isPositive();
    }
}
