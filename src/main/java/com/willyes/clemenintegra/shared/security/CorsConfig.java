package com.willyes.clemenintegra.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    };

    private static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type"
    };

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(ALLOWED_ORIGINS)
                        .allowedMethods(ALLOWED_METHODS)
                        .allowedHeaders(ALLOWED_HEADERS)
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of(ALLOWED_ORIGINS));
        configuration.setAllowedMethods(java.util.List.of(ALLOWED_METHODS));
        configuration.setAllowedHeaders(java.util.List.of(ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
