package com.willyes.clemenintegra.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ForwardHeadersIntegrationTest.ForwardedSchemeController.class)
@TestPropertySource(properties = {
        "server.forward-headers-strategy=framework"
})
@Import({ForwardHeadersIntegrationTest.ForwardedSchemeController.class, ForwardHeadersIntegrationTest.TestSecurityConfig.class})
class ForwardHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void respectsForwardedProtoHttps() throws Exception {
        mockMvc.perform(get("/test/forwarded-scheme")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "example.com")
                        .header("X-Forwarded-Port", "443"))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("https://example.com/test/forwarded-scheme")));
    }

    @RestController
    public static class ForwardedSchemeController {

        @GetMapping("/test/forwarded-scheme")
        public String currentRequestUri(HttpServletRequest request) {
            return ServletUriComponentsBuilder.fromCurrentRequest()
                    .build()
                    .toUriString();
        }
    }

    @Configuration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        ForwardedHeaderFilter forwardedHeaderFilter() {
            return new ForwardedHeaderFilter();
        }
    }
}
