package com.jerry.salesmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTests {

    @Test
    void defaultsToLocalhostAndLoopbackOriginsWithCredentialsAndContentDispositionExposed() {
        CorsConfig config = new CorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals(List.of("http://localhost:3000", "http://127.0.0.1:3000"), cors.getAllowedOriginPatterns());
        assertFalse(cors.getAllowedOriginPatterns().contains("*"));
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertEquals(List.of("Content-Disposition"), cors.getExposedHeaders());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), cors.getAllowedMethods());
    }

    @Test
    void parsesMultipleCommaSeparatedOriginPatternsAndFallsBackWhenBlank() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOrigins",
                " https://erp.example.com , http://192.168.*:3000 , "
        );

        assertEquals(List.of("https://erp.example.com", "http://192.168.*:3000"), config.resolveAllowedOrigins());

        ReflectionTestUtils.setField(config, "allowedOrigins", "   ");
        assertEquals(List.of("http://localhost:3000", "http://127.0.0.1:3000"), config.resolveAllowedOrigins());
    }
}
