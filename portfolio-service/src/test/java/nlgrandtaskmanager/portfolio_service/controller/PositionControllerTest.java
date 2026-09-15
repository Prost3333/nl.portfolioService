package nlgrandtaskmanager.portfolio_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nlgrandtaskmanager.portfolio_service.config.SecurityConfig;
import nlgrandtaskmanager.portfolio_service.dto.PositionResponse;
import nlgrandtaskmanager.portfolio_service.security.JwtAuthenticationFilter;
import nlgrandtaskmanager.portfolio_service.security.JwtService;
import nlgrandtaskmanager.portfolio_service.service.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PositionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PositionService positionService;

    @MockitoBean
    private JwtService jwtService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }


    // --- GET /positions ---

    @Test
    void getPositions_returns200_withListOfPositions() throws Exception {
        PositionResponse p1 = new PositionResponse(UUID.randomUUID(), "AAPL", "Apple", BigDecimal.TEN, Instant.now());
        PositionResponse p2 = new PositionResponse(UUID.randomUUID(), "GOOG", "Google", BigDecimal.ONE, Instant.now());

        when(positionService.getPositions(USER_ID)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/positions")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[1].ticker").value("GOOG"));
    }

    @Test
    void getPositions_returns200_withEmptyList() throws Exception {
        when(positionService.getPositions(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/positions")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPositions_returns403_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/positions"))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /positions/{id} ---

    @Test
    void deletePosition_returns204_whenOwnerDeletes() throws Exception {
        UUID positionId = UUID.randomUUID();

        mockMvc.perform(delete("/positions/{id}", positionId)
                        .with(authentication(auth())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePosition_returns404_whenPositionNotFound() throws Exception {
        UUID positionId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"))
                .when(positionService).delete(USER_ID, positionId);

        mockMvc.perform(delete("/positions/{id}", positionId)
                        .with(authentication(auth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePosition_returns403_whenNotOwner() throws Exception {
        UUID positionId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"))
                .when(positionService).delete(USER_ID, positionId);

        mockMvc.perform(delete("/positions/{id}", positionId)
                        .with(authentication(auth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePosition_returns403_withoutAuthentication() throws Exception {
        mockMvc.perform(delete("/positions/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
