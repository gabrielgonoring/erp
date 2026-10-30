package company.aura.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void shouldConfigureDefaultAllowedOrigin() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertNotNull(config.getAllowedOriginPatterns());
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:5173"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertTrue(config.getAllowCredentials());
        assertTrue(config.getAllowedHeaders().contains("*"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
    }

    @Test
    void shouldSupportMultipleOriginsConfiguredViaCommaSeparatedString() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173, https://app.example.com ");

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/test");
        request1.addHeader("Origin", "http://localhost:5173");
        CorsConfiguration config1 = source.getCorsConfiguration(request1);
        assertNotNull(config1);
        assertEquals("http://localhost:5173", config1.checkOrigin("http://localhost:5173"));

        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/test");
        request2.addHeader("Origin", "https://app.example.com");
        CorsConfiguration config2 = source.getCorsConfiguration(request2);
        assertNotNull(config2);
        assertEquals("https://app.example.com", config2.checkOrigin("https://app.example.com"));

        assertNull(config2.checkOrigin("http://unauthorized-domain.com"));
    }
}
