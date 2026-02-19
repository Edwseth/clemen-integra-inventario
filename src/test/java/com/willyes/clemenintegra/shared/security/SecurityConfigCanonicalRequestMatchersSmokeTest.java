package com.willyes.clemenintegra.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
class SecurityConfigCanonicalRequestMatchersSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;


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
        assertNotForbidden(mockMvc.perform(get("/api/produccion/ordenes/1")
                .with(SecurityMockMvcRequestPostProcessors.user("prod-op-read").authorities(() -> "PROD_OP_READ"))));

        assertForbidden(mockMvc.perform(get("/api/produccion/ordenes/1")
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
        assertNotForbidden(mockMvc.perform(get("/api/admin/rbac/roles")
                .with(SecurityMockMvcRequestPostProcessors.user("rbac-read").authorities(() -> "ADMIN_RBAC_READ"))));

        assertForbidden(mockMvc.perform(get("/api/admin/rbac/roles")
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
