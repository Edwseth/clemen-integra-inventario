package com.willyes.clemenintegra.shared.security;

import com.willyes.clemenintegra.shared.logging.RequestIdFilter;
import com.willyes.clemenintegra.shared.performance.RequestTimingFilter;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigCanonicalRequestMatchersSmokeTest.SmokeEndpointsController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class SecurityConfigCanonicalRequestMatchersSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private RequestTimingFilter requestTimingFilter;
    @MockBean
    private RequestIdFilter requestIdFilter;
    @MockBean
    private SuperAdminSoloLecturaWriteBlockFilter superAdminSoloLecturaWriteBlockFilter;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setupFilters() throws Exception {
        passthrough(usuarioInactivoFilter);
        passthrough(requestTimingFilter);
        passthrough(requestIdFilter);
        passthrough(superAdminSoloLecturaWriteBlockFilter);
        passthrough(jwtAuthenticationFilter);
    }

    private void passthrough(jakarta.servlet.Filter filter) throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(filter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    void inv_solicitudes_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(get("/api/inventario/solicitudes/demo")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_ALMACENES")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventario/solicitudes/demo")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "INV_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void po_planeacion_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(get("/api/planeacion/planes-semanales")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_PLANEADOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/planeacion/planes-semanales")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "PO_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void prod_ordenes_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/produccion/ordenes")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "PROD_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void qc_retenciones_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(get("/api/calidad/retenciones/test")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_CALIDAD")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/calidad/retenciones/test")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "QC_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void bom_formulaActiva_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(get("/api/bom/formulas/activa")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_PRODUCCION")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/bom/formulas/activa")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "BOM_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void doc_delete_requiresCanonicalPermission() throws Exception {
        mockMvc.perform(delete("/api/documental/documentos/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_CALIDAD")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/documental/documentos/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "DOC_DELETE")))
                .andExpect(status().isOk());
    }


    @Test
    void qc_capas_get_and_post_require_qc_permissions_only() throws Exception {
        mockMvc.perform(get("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "QC_READ")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "PO_READ")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "QC_READ")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/calidad/capas")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "QC_WRITE")))
                .andExpect(status().isOk());
    }

    @Test
    void inventario_lotes_por_evaluar_requires_inventory_permission_not_role() throws Exception {
        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "ROL_JEFE_CALIDAD")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lotes/por-evaluar")
                        .with(SecurityMockMvcRequestPostProcessors.user("u").authorities(() -> "INV_LOTES_READ")))
                .andExpect(status().isOk());
    }
    @RestController
    @RequestMapping("/api")
    static class SmokeEndpointsController {

        @GetMapping("/inventario/solicitudes/demo")
        ResponseEntity<Void> invSolicitudes() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/planeacion/planes-semanales")
        ResponseEntity<Void> poPlaneacion() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/produccion/ordenes")
        ResponseEntity<Void> prodOrdenes() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/calidad/retenciones/test")
        ResponseEntity<Void> qcRetenciones() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/bom/formulas/activa")
        ResponseEntity<Void> bomActiva() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/calidad/capas")
        ResponseEntity<Void> qcCapasGet() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/calidad/capas")
        ResponseEntity<Void> qcCapasPost() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/lotes/por-evaluar")
        ResponseEntity<Void> lotesPorEvaluar() {
            return ResponseEntity.ok().build();
        }

        @DeleteMapping("/documental/documentos/1")
        ResponseEntity<Void> docDelete() {
            return ResponseEntity.ok().build();
        }
    }
}
