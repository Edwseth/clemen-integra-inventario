package com.willyes.clemenintegra.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "https://*.ngrok-free.app",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    };

    private static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "Accept", "X-Bypass-Auth-Redirect",
            "ngrok-skip-browser-warning"
    };

    private static final String[] EXPOSED_HEADERS = {
            "Content-Disposition"
    };

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                        .allowedMethods(ALLOWED_METHODS)
                        .allowedHeaders(ALLOWED_HEADERS)
                        .exposedHeaders(EXPOSED_HEADERS)
                        .allowCredentials(true);
            }
        };
    }
}
