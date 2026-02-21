package com.willyes.clemenintegra.inventario.regularizacion.controller;

import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.VariacionOPResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.service.variaciones.VariacionesProduccionService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.SuperAdminSoloLecturaWriteBlockFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VariacionesProduccionController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, VariacionesProduccionControllerSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class VariacionesProduccionControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VariacionesProduccionService variacionesService;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void configureFilters() throws Exception {
        doAnswer(invocation -> {
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(usuarioInactivoFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestTimingFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestIdFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            var chain = invocation.getArgument(2, jakarta.servlet.FilterChain.class);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(superAdminSoloLecturaWriteBlockFilter).doFilter(any(), any(), any());
    }

    @Test
    void readSinPermisoRetorna403() throws Exception {
        when(variacionesService.listarVariaciones(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(VariacionOPResponseDTO.builder()
                        .regularizacionId(1L)
                        .ordenProduccionId(10L)
                        .fechaIngreso(LocalDateTime.now())
                        .build()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/produccion/variaciones")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permiso").authorities(() -> "PROD_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportSinPermisoRetorna403() throws Exception {
        mockMvc.perform(get("/api/produccion/variaciones/export")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permiso").authorities(() -> "PROD_VARIACIONES_READ")))
                .andExpect(status().isForbidden());
    }
}
