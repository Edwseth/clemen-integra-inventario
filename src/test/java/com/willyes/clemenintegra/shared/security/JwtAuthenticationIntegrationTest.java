package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.security.exception.SesionExpiradaAuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JwtAuthenticationIntegrationTest.ProtectedController.class)
@Import({JwtAuthenticationIntegrationTest.TestSecurityConfig.class, JwtAuthenticationIntegrationTest.ProtectedController.class})
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void responde401ConSesionExpiradaCuandoTokenEsInactivo() throws Exception {
        mockMvc.perform(get("/api/test")
                        .header("Authorization", "Bearer inactive-token")
                        .header(RequestIdFilter.HEADER_NAME, "req-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESION_EXPIRADA"))
                .andExpect(jsonPath("$.message")
                        .value("Tu sesión expiró por inactividad. Inicia sesión nuevamente."))
                .andExpect(jsonPath("$.requestId").value("req-123"));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/test")
        public String test() {
            return "ok";
        }
    }

    @Configuration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                JwtAuthenticationFilter jwtAuthenticationFilter,
                                                RequestIdFilter requestIdFilter,
                                                ApiAuthenticationEntryPoint entryPoint) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/test").authenticated()
                            .anyRequest().permitAll()
                    )
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));

            http.addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(ObjectProvider<AuthenticationManager> authenticationManagerProvider,
                                                        ApiAuthenticationEntryPoint entryPoint) {
            return new JwtAuthenticationFilter(authenticationManagerProvider, entryPoint);
        }

        @Bean
        ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
            return new ApiAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        AuthenticationManager authenticationManager() {
            return authentication -> {
                throw new SesionExpiradaAuthenticationException("La sesión ha expirado por inactividad.");
            };
        }

        @Bean
        RequestIdFilter requestIdFilter() {
            return new RequestIdFilter();
        }
    }
}
