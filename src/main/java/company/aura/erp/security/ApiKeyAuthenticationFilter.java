package company.aura.erp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final JobSecurityProperties jobSecurityProperties;
    private final ApiKeyAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        if (StringUtils.isBlank(path)) {
            path = request.getRequestURI();
        }
        return !path.startsWith("/api/jobs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String headerName = jobSecurityProperties.getHeaderName();
        String requestApiKey = request.getHeader(headerName);

        if (StringUtils.isBlank(requestApiKey) && !headerName.equalsIgnoreCase("X-API-KEY")) {
            requestApiKey = request.getHeader("X-API-KEY");
        }

        String expectedApiKey = jobSecurityProperties.getApiKey();

        if (StringUtils.isBlank(requestApiKey) || !requestApiKey.equals(expectedApiKey)) {
            log.warn("Tentativa de acesso não autorizada ao endpoint de jobs: {} - API Key inválida ou ausente", request.getRequestURI());
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("API Key ausente ou inválida.")
            );
            return;
        }

        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                requestApiKey,
                List.of(new SimpleGrantedAuthority("ROLE_JOB"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
