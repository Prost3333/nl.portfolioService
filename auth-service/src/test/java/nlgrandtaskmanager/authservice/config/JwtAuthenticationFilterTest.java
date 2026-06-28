package nlgrandtaskmanager.authservice.config;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nlgrandtaskmanager.authservice.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_whenNoAuthHeader_passesRequestThrough()throws Exception{
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request,response,filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).parseUserId(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

    }

    @Test
    void doFilterInternal_whenValidToken_setsAuthenticationInContext() throws Exception {
        UUID expectedUserId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.parseUserId("valid-token")).thenReturn(expectedUserId);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService).parseUserId("valid-token");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(expectedUserId, auth.getPrincipal());
    }

    @Test
    void doFilterInternal_whenInvalidToken_returnsUnauthorized() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer Error/token");
        when(jwtService.parseUserId("Error/token"))
                .thenThrow(new JwtException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
