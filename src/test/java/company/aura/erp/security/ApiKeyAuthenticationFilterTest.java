package company.aura.erp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private JobSecurityProperties jobSecurityProperties;

    @Mock
    private ApiKeyAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("shouldNotFilter — Deve ignorar rotas que não iniciam com /api/jobs")
    void shouldSkipNonJobRoutes() {
        when(request.getServletPath()).thenReturn("/api/orders");
        assertTrue(apiKeyAuthenticationFilter.shouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/auth/login");
        assertTrue(apiKeyAuthenticationFilter.shouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/jobs/orders/receivable/notify");
        assertFalse(apiKeyAuthenticationFilter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("Deve rejeitar requisição para rota de jobs sem header de API Key")
    void shouldRejectWhenApiKeyHeaderMissing() throws ServletException, IOException {
        when(jobSecurityProperties.getHeaderName()).thenReturn("X-API-KEY");
        when(jobSecurityProperties.getApiKey()).thenReturn("secret-key");
        when(request.getHeader("X-API-KEY")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/jobs/orders/receivable/notify");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint, times(1)).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve rejeitar requisição para rota de jobs com API Key inválida")
    void shouldRejectWhenApiKeyInvalid() throws ServletException, IOException {
        when(jobSecurityProperties.getHeaderName()).thenReturn("X-API-KEY");
        when(jobSecurityProperties.getApiKey()).thenReturn("secret-key");
        when(request.getHeader("X-API-KEY")).thenReturn("wrong-key");
        when(request.getRequestURI()).thenReturn("/api/jobs/orders/receivable/notify");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(authenticationEntryPoint, times(1)).commence(eq(request), eq(response), any());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve autenticar requisição para rota de jobs com API Key válida")
    void shouldAuthenticateWhenApiKeyValid() throws ServletException, IOException {
        when(jobSecurityProperties.getHeaderName()).thenReturn("X-API-KEY");
        when(jobSecurityProperties.getApiKey()).thenReturn("valid-secret-key");
        when(request.getHeader("X-API-KEY")).thenReturn("valid-secret-key");

        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(authenticationEntryPoint);
        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("JOB_API_CLIENT", auth.getPrincipal());
        assertEquals("valid-secret-key", auth.getCredentials());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JOB")));
    }
}
