package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.SuperAdminSoloLecturaWriteBlockFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SolicitudPorOrdenController.class, SolicitudMovimientoController.class})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SolicitudMovimientoControllerSecurityTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SolicitudMovimientoControllerSecurityTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolicitudMovimientoService solicitudMovimientoService;
    @MockBean
    private UsuarioService usuarioService;
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
    void configureFilters() throws ServletException, IOException {
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
    }

    @Test
    void planeadorPuedeListarSolicitudesPorOrden() throws Exception {
        when(solicitudMovimientoService.listGroupByOrden(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/inventarios/solicitudes/por-orden")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user("planeador")
                                .authorities(() -> "ROL_PLANEADOR")))
                .andExpect(status().isOk());
    }


    @Test
    void jefeProduccionPuedeVerDetalleSolicitud() throws Exception {
        when(solicitudMovimientoService.obtenerSolicitud(1L))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(1L).build());

        mockMvc.perform(get("/api/inventario/solicitudes/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe-produccion")
                                .authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isOk());
    }

    @Test
    void jefeProduccionRecibeNotFoundSiSolicitudNoExiste() throws Exception {
        when(solicitudMovimientoService.obtenerSolicitud(999L))
                .thenThrow(new EntityNotFoundException("Solicitud no encontrada: 999"));

        mockMvc.perform(get("/api/inventario/solicitudes/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe-produccion")
                                .authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorOrdenYConsultarDetalleEnRutaPluralRespondeOk() throws Exception {
        when(solicitudMovimientoService.listGroupByOrden(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(solicitudMovimientoService.obtenerSolicitud(321L))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(321L).build());

        mockMvc.perform(get("/api/inventarios/solicitudes/por-orden")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe-produccion")
                                .authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventarios/solicitudes/321")
                        .with(SecurityMockMvcRequestPostProcessors.user("jefe-produccion")
                                .authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isOk());
    }

    @Test
    void planeadorNoPuedeAprobarSolicitudes() throws Exception {
        mockMvc.perform(put("/api/inventario/solicitudes/1/aprobar")
                        .with(SecurityMockMvcRequestPostProcessors.user("planeador")
                                .authorities(() -> "ROL_PLANEADOR")))
                .andExpect(status().isForbidden());
    }
}
