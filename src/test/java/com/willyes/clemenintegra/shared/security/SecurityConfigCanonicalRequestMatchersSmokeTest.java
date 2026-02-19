package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.calidad.controller.CapaController;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.service.CapaService;
import com.willyes.clemenintegra.produccion.controller.AlistamientoProduccionController;
import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionAlistamientoService;
import com.willyes.clemenintegra.shared.security.controller.AdminRbacController;
import com.willyes.clemenintegra.shared.security.service.RbacAdminService;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CapaController.class,
        AlistamientoProduccionController.class,
        AdminRbacController.class
})
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityConfigCanonicalRequestMatchersSmokeTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SecurityConfigCanonicalRequestMatchersSmokeTest {

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CapaService capaService;
    @MockBean
    private ProduccionAlistamientoService produccionAlistamientoService;
    @MockBean
    private RbacAdminService rbacAdminService;

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

        when(capaService.listar(any(), any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(capaService.crear(any(CapaDTO.class))).thenReturn(new CapaDTO());
        when(produccionAlistamientoService.obtenerAlistamientoPorOrden(1L)).thenReturn(AlistamientoOrdenProduccionDTO.builder().build());
        when(rbacAdminService.listarModulosPermisosActivos()).thenReturn(List.of());
    }

    @Test
    void qcReadPermiteGetCapasPeroNoPost_yQcWritePermitePost() throws Exception {
        // endpoint real /api/calidad/capas: GET permitido por QC_READ en SecurityConfig y @PreAuthorize; POST requiere QC_WRITE+.
        mockMvc.perform(get("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("qc-read").authorities(() -> "QC_READ")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/calidad/capas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(SecurityMockMvcRequestPostProcessors.user("qc-read").authorities(() -> "QC_READ")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/calidad/capas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(SecurityMockMvcRequestPostProcessors.user("qc-write").authorities(() -> "QC_WRITE")))
                .andExpect(status().isCreated());
    }

    @Test
    void prodOpReadPermiteGetOrdenesYSinPermisoDa403() throws Exception {
        // endpoint real /api/produccion/ordenes/{id}/alistamiento: SecurityConfig GET /api/produccion/ordenes/** acepta PROD_READ|PROD_OP_READ.
        mockMvc.perform(get("/api/produccion/ordenes/1/alistamiento")
                        .with(SecurityMockMvcRequestPostProcessors.user("prod-op-read").authorities(() -> "PROD_OP_READ")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/produccion/ordenes/1/alistamiento")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos").authorities(() -> "QC_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRbacReadProtegeEndpointYSinPermisoDa403() throws Exception {
        // endpoint real /api/admin/rbac/modulos: SecurityConfig GET exige ADMIN_RBAC_READ|ADMIN_RBAC_WRITE y controller refuerza lo mismo.
        mockMvc.perform(get("/api/admin/rbac/modulos")
                        .with(SecurityMockMvcRequestPostProcessors.user("rbac-read").authorities(() -> "ADMIN_RBAC_READ")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/rbac/modulos")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-rbac").authorities(() -> "DOC_READ")))
                .andExpect(status().isForbidden());
    }
}
