package com.willyes.clemenintegra.gerencial.controller;

import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.gerencial.service.SeguimientoGerencialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SeguimientoGerencialController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SeguimientoGerencialControllerTest.MethodSecurityTestConfig.class)
class SeguimientoGerencialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeguimientoGerencialService seguimientoGerencialService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = {"ROL_GERENCIAL", "GER_SEGUIMIENTO_READ"})
    void endpointDebeResponderSummaryEItems() throws Exception {
        SeguimientoGerencialResponseDTO payload = SeguimientoGerencialResponseDTO.builder()
                .summary(SeguimientoGerencialResponseDTO.SummaryDTO.builder().totalItems(1).build())
                .items(List.of(SeguimientoGerencialResponseDTO.ItemDTO.builder().planDetalleId(10L).build()))
                .build();

        when(seguimientoGerencialService.obtenerSeguimiento(7L)).thenReturn(payload);

        mockMvc.perform(get("/api/gerencial/planes-semanales/7/seguimiento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].planDetalleId").value(10));
    }

    @Test
    @WithMockUser(authorities = "PO_READ")
    void endpointGerencialRechazaUsuarioAutenticadoSinPermisoCanonico() throws Exception {
        mockMvc.perform(get("/api/gerencial/planes-semanales/7/seguimiento"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
