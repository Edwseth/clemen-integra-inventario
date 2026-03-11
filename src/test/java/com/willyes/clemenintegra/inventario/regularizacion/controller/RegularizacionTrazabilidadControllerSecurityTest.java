package com.willyes.clemenintegra.inventario.regularizacion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.service.RegularizacionTrazabilidadService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.model.Usuario;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegularizacionTrazabilidadController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, RegularizacionTrazabilidadControllerSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class RegularizacionTrazabilidadControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegularizacionTrazabilidadService service;
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
    void permiteRolContador() throws Exception {
        when(service.regularizarPorOP(any(), anyString(), any(Usuario.class)))
                .thenReturn(RegularizacionTrazabilidadResponseDTO.builder()
                         .regularizacionId(1L)
                        .idempotencyKey("k1")
                        .ordenProduccionId(1L)
                                                .movimientos(List.of())
                        .registradoPorId(1L)
                        .fecha(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/produccion/regularizaciones")
                        .header("Idempotency-Key", "k1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request()))
                        .with(SecurityMockMvcRequestPostProcessors.user("contador").authorities(() -> "PROD_TRAZABILIDAD_REGULARIZACION")))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaRolDistinto() throws Exception {
        mockMvc.perform(post("/api/produccion/regularizaciones")
                        .header("Idempotency-Key", "k1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request()))
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe").authorities(() -> "ROL_JEFE_ALMACENES")))
                .andExpect(status().isForbidden());
    }

    @Test
    void retornaConflictCuandoOpYaFueRegularizada() throws Exception {
        when(service.regularizarPorOP(any(), anyString(), any(Usuario.class)))
                .thenThrow(new CustomBusinessException(
                        ApiErrorCode.OPERACION_NO_PERMITIDA,
                        "La orden de producción ya tiene una regularización registrada"));

        mockMvc.perform(post("/api/produccion/regularizaciones")
                        .header("Idempotency-Key", "k1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request()))
                        .with(SecurityMockMvcRequestPostProcessors.user("contador").authorities(() -> "PROD_TRAZABILIDAD_REGULARIZACION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La orden de producción ya tiene una regularización registrada"));
    }

    private RegularizacionTrazabilidadRequestDTO request() {
        return new RegularizacionTrazabilidadRequestDTO(
                1L,
                BigDecimal.TEN,
                "DOC-1",
                "Observaciones válidas para seguridad",
                Boolean.FALSE
        );
    }
}
