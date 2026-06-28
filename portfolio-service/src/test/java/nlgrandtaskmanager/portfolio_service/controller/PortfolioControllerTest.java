package nlgrandtaskmanager.portfolio_service.controller;

import nlgrandtaskmanager.portfolio_service.config.SecurityConfig;
import nlgrandtaskmanager.portfolio_service.dto.PortfolioSummaryResponse;
import nlgrandtaskmanager.portfolio_service.dto.PositionValue;
import nlgrandtaskmanager.portfolio_service.security.JwtAuthenticationFilter;
import nlgrandtaskmanager.portfolio_service.security.JwtService;
import nlgrandtaskmanager.portfolio_service.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private JwtService jwtService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    @Test
    void getSummary_returns200_withPortfolioData() throws Exception {
        PositionValue positionValue = new PositionValue(
                "AAPL", "Apple", BigDecimal.TEN,
                new BigDecimal("150"), new BigDecimal("1500"),
                new BigDecimal("100"), true
        );
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
                new BigDecimal("1500"), List.of(positionValue)
        );

        when(portfolioService.getSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/portfolio/summary")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(1500))
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.positions[0].price").value(150))
                .andExpect(jsonPath("$.positions[0].value").value(1500))
                .andExpect(jsonPath("$.positions[0].allocation").value(100))
                .andExpect(jsonPath("$.positions[0].priceAvailable").value(true));
    }

    @Test
    void getSummary_returns200_withEmptyPortfolio() throws Exception {
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(BigDecimal.ZERO, List.of());

        when(portfolioService.getSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/portfolio/summary")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(0))
                .andExpect(jsonPath("$.positions.length()").value(0));
    }

    @Test
    void getSummary_returns200_withUnavailablePrice() throws Exception {
        PositionValue positionValue = new PositionValue(
                "UNKNOWN", "Unknown Corp", BigDecimal.TEN,
                null, null, BigDecimal.ZERO, false
        );
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
                BigDecimal.ZERO, List.of(positionValue)
        );

        when(portfolioService.getSummary(USER_ID)).thenReturn(summary);

        mockMvc.perform(get("/portfolio/summary")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].priceAvailable").value(false))
                .andExpect(jsonPath("$.positions[0].price").doesNotExist());
    }

    @Test
    void getSummary_returns403_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/portfolio/summary"))
                .andExpect(status().isForbidden());
    }
}
