package com.willyes.clemenintegra.shared.security.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.willyes.clemenintegra.shared.security.dto.rbac.PermisoDTO;
import com.willyes.clemenintegra.shared.security.dto.rbac.RolDTO;
import com.willyes.clemenintegra.shared.security.service.RbacAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminRbacController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminRbacControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RbacAdminService rbacAdminService;

    @Test
    void getRolesRetornaListado() throws Exception {
        when(rbacAdminService.listarRoles(false)).thenReturn(List.of(new RolDTO(1L, "ROL_SUPER_ADMIN", "Super", true)));

        mockMvc.perform(get("/api/admin/rbac/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ROL_SUPER_ADMIN"));
    }

    @Test
    void getPermisosInvRetornaCatalogoCanonico() throws Exception {
        when(rbacAdminService.listarPermisos("INV", true))
                .thenReturn(List.of(new PermisoDTO(10L, "INV_READ", "Lectura", "INV", true, "READ")));

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "INV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("INV_READ"))
                .andExpect(jsonPath("$[0].modulo").value("INV"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }

    @Test
    void getPermisosQcRetornaCatalogoCanonico() throws Exception {
        when(rbacAdminService.listarPermisos("QC", true))
                .thenReturn(List.of(
                        new PermisoDTO(20L, "QC_READ", "Lectura", "QC", true, "READ"),
                        new PermisoDTO(21L, "QC_WORKFLOW_FINISH", "Cierre", "QC", true, "WORKFLOW_FINISH")
                ));

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "QC")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("QC_READ"))
                .andExpect(jsonPath("$[0].modulo").value("QC"))
                .andExpect(jsonPath("$[1].codigo").value("QC_WORKFLOW_FINISH"));
    }


    @Test
    void getPermisosPoRetornaCatalogoCanonico() throws Exception {
        when(rbacAdminService.listarPermisos("PO", true))
                .thenReturn(List.of(new PermisoDTO(30L, "PO_READ", "Lectura", "PO", true, "READ")));

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "PO")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PO_READ"))
                .andExpect(jsonPath("$[0].modulo").value("PO"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }


    @Test
    void getPermisosBomRetornaCatalogoCanonico() throws Exception {
        when(rbacAdminService.listarPermisos("BOM", true))
                .thenReturn(List.of(new PermisoDTO(40L, "BOM_READ", "Lectura", "BOM", true, "READ")));

        mockMvc.perform(get("/api/admin/rbac/permisos")
                        .param("modulo", "BOM")
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("BOM_READ"))
                .andExpect(jsonPath("$[0].modulo").value("BOM"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }


}
