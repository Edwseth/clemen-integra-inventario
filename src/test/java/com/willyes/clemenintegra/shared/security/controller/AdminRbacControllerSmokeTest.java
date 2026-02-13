package com.willyes.clemenintegra.shared.security.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
