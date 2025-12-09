package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = PlanProduccionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PlanProduccionControllerTest.MethodSecurityTestConfig.class)
class PlanProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanProduccionService planProduccionService;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    void crearPlanPermiteJefeProduccion() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .detalles(List.of())
                .build();
        when(planProduccionService.crearOActualizar(any())).thenReturn(plan);

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .content("{}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarPlanPermiteSuperAdmin() throws Exception {
        PlanProduccionResumenDTO plan = PlanProduccionResumenDTO.builder()
                .id(2L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .semanaInicio(LocalDate.of(2024, 1, 1))
                .semanaFin(LocalDate.of(2024, 1, 7))
                .build();
        when(planProduccionService.listar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(plan)));

        mockMvc.perform(get("/api/planeacion/planes-semanales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].estado").value("CONFIRMADO"));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    void listarConFiltrosPasaParametrosAlServicio() throws Exception {
        when(planProduccionService.listar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/planeacion/planes-semanales")
                        .param("semanaInicioDesde", "2024-04-01")
                        .param("semanaInicioHasta", "2024-04-07")
                        .param("estado", "BORRADOR"))
                .andExpect(status().isOk());

        verify(planProduccionService).listar(eq(LocalDate.of(2024, 4, 1)), eq(LocalDate.of(2024, 4, 7)), eq(EstadoPlanProduccion.BORRADOR), any());
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void crearPlanRechazaComprador() throws Exception {
        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .content("{}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
