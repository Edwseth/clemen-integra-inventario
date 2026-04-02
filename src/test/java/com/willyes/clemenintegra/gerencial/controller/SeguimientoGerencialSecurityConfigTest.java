package com.willyes.clemenintegra.gerencial.controller;

import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.gerencial.service.SeguimientoGerencialService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.SuperAdminSoloLecturaWriteBlockFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SeguimientoGerencialController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SeguimientoGerencialSecurityConfigTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SeguimientoGerencialSecurityConfigTest {

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeguimientoGerencialService seguimientoGerencialService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.repository.UsuarioRepository usuarioRepository;
    @MockBean
    private com.willyes.clemenintegra.shared.performance.RequestTimingFilter requestTimingFilter;
    @MockBean
    private com.willyes.clemenintegra.shared.logging.RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;

    @BeforeEach
    void configureFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestTimingFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestIdFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(superAdminSoloLecturaWriteBlockFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        SeguimientoGerencialResponseDTO payload = SeguimientoGerencialResponseDTO.builder()
                .summary(SeguimientoGerencialResponseDTO.SummaryDTO.builder().totalItems(1).build())
                .items(List.of())
                .build();
        when(seguimientoGerencialService.obtenerSeguimiento(7L)).thenReturn(payload);
    }

    @Test
    void endpointGerencialRequierePermisoCanonico() throws Exception {
        mockMvc.perform(get("/api/gerencial/planes-semanales/7/seguimiento")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permiso")
                                .authorities(() -> "PO_READ")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/gerencial/planes-semanales/7/seguimiento")
                        .with(SecurityMockMvcRequestPostProcessors.user("gerencial")
                                .authorities(() -> "ROL_GERENCIAL", () -> "GER_SEGUIMIENTO_READ")))
                .andExpect(status().isOk());
    }
}
