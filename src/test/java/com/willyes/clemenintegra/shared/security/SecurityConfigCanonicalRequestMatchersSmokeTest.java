package com.willyes.clemenintegra.shared.security;

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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigCanonicalRequestMatchersSmokeTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityConfigCanonicalRequestMatchersSmokeTest.MethodSecurityConfig.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SecurityConfigCanonicalRequestMatchersSmokeTest {

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityConfig {
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/api/calidad/capas", "/api/inventario/productos/1", "/api/inventario/lotes/1", "/api/produccion/ordenes", "/api/documental/documentos", "/api/admin/rbac/modulos"})
        public String getProbe() {
            return "ok";
        }

        @PostMapping({"/api/calidad/capas", "/api/documental/documentos"})
        public String postProbe() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

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
    }

    @Test
    void qcReadPermiteGetCapasPeroNoPost_yQcWritePermitePost() throws Exception {
        assertNotForbidden(mockMvc.perform(get("/api/calidad/capas")
                .with(SecurityMockMvcRequestPostProcessors.user("qc-read").authorities(() -> "QC_READ"))));

        assertForbidden(mockMvc.perform(post("/api/calidad/capas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(SecurityMockMvcRequestPostProcessors.user("qc-read").authorities(() -> "QC_READ"))));

        assertNotForbidden(mockMvc.perform(post("/api/calidad/capas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(SecurityMockMvcRequestPostProcessors.user("qc-write").authorities(() -> "QC_WRITE"))));
    }

    @Test
    void invPermisosLecturaProtegenProductosYLotes() throws Exception {
        assertNotForbidden(mockMvc.perform(get("/api/inventario/productos/1")
                .with(SecurityMockMvcRequestPostProcessors.user("inv-product-read").authorities(() -> "INV_PRODUCT_READ"))));

        assertNotForbidden(mockMvc.perform(get("/api/inventario/lotes/1")
                .with(SecurityMockMvcRequestPostProcessors.user("inv-lotes-read").authorities(() -> "INV_LOTES_READ"))));
    }

    @Test
    void prodOpReadPermiteGetOrdenesYSinPermisoDa403() throws Exception {
        assertNotForbidden(mockMvc.perform(get("/api/produccion/ordenes")
                .with(SecurityMockMvcRequestPostProcessors.user("prod-op-read").authorities(() -> "PROD_OP_READ"))));

        assertForbidden(mockMvc.perform(get("/api/produccion/ordenes")
                .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos").authorities(() -> "QC_READ"))));
    }

    @Test
    void documentalDocReadPermiteGetYDocWritePermitePost() throws Exception {
        assertNotForbidden(mockMvc.perform(get("/api/documental/documentos")
                .with(SecurityMockMvcRequestPostProcessors.user("doc-read").authorities(() -> "DOC_READ"))));

        assertNotForbidden(mockMvc.perform(post("/api/documental/documentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(SecurityMockMvcRequestPostProcessors.user("doc-write").authorities(() -> "DOC_WRITE"))));
    }

    @Test
    void adminRbacReadProtegeEndpointYSinPermisoDa403() throws Exception {
        assertNotForbidden(mockMvc.perform(get("/api/admin/rbac/modulos")
                .with(SecurityMockMvcRequestPostProcessors.user("rbac-read").authorities(() -> "ADMIN_RBAC_READ"))));

        assertForbidden(mockMvc.perform(get("/api/admin/rbac/modulos")
                .with(SecurityMockMvcRequestPostProcessors.user("sin-rbac").authorities(() -> "DOC_READ"))));
    }

    private void assertForbidden(ResultActions action) throws Exception {
        action.andExpect(status().isForbidden());
    }

    private void assertNotForbidden(ResultActions action) throws Exception {
        MockHttpServletResponse response = action.andReturn().getResponse();
        assertThat(response.getStatus()).isNotEqualTo(403);
    }
}
