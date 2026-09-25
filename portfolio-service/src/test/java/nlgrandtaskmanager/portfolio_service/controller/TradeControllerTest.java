package nlgrandtaskmanager.portfolio_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nlgrandtaskmanager.portfolio_service.config.SecurityConfig;
import nlgrandtaskmanager.portfolio_service.dto.CreateTradeRequest;
import nlgrandtaskmanager.portfolio_service.dto.TradeResponse;
import nlgrandtaskmanager.portfolio_service.enums.TradeType;
import nlgrandtaskmanager.portfolio_service.security.JwtAuthenticationFilter;
import nlgrandtaskmanager.portfolio_service.security.JwtService;
import nlgrandtaskmanager.portfolio_service.service.TradeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class TradeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TradeService tradeService;

    @MockitoBean
    private JwtService jwtService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    private List<TradeResponse> getList() {
        TradeResponse t1 = new TradeResponse(UUID.randomUUID(), "APPL", TradeType.BUY, BigDecimal.valueOf(5)
                , BigDecimal.valueOf(115), LocalDate.now(), null, null, null);
        TradeResponse t2 = new TradeResponse(UUID.randomUUID(), "TESL", TradeType.BUY, BigDecimal.valueOf(10)
                , BigDecimal.valueOf(435), LocalDate.now(), null, null, null);
        return List.of(t1, t2);
    }

    @Test
    void getTrade_returns200() throws Exception {
        when(tradeService.getTrades(USER_ID)).thenReturn(getList());

        mockMvc.perform(get("/trade/trades")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("APPL"))
                .andExpect(jsonPath("$[1].ticker").value("TESL"));
    }

    @Test
    void getTrade_returns403() throws Exception {
        mockMvc.perform(get("/trade/trades"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addTrade_returnsBadRequest_whenQuantityIsNegative() throws Exception {
        CreateTradeRequest request = new CreateTradeRequest(
                "AAPL",
                BigDecimal.valueOf(-5),
                TradeType.BUY,
                BigDecimal.valueOf(115),
                LocalDate.now(),
                null, null, null
        );

        mockMvc.perform(post("/trade/trades")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(tradeService, never()).addTrade(any(), any());
    }
    @Test
    void addTrade_returns201_andPassesAuthenticatedUserId() throws Exception {
        CreateTradeRequest request = new CreateTradeRequest(
                "AAPL", BigDecimal.valueOf(5), TradeType.BUY,
                BigDecimal.valueOf(115), LocalDate.of(2026, 1, 15),
                "Отчётность лучше ожиданий", 4, null);

        mockMvc.perform(post("/trade/trades")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<CreateTradeRequest> captor = ArgumentCaptor.forClass(CreateTradeRequest.class);
        verify(tradeService).addTrade(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().ticker()).isEqualTo("AAPL");
        assertThat(captor.getValue().quantity()).isEqualByComparingTo("5");
    }

}




