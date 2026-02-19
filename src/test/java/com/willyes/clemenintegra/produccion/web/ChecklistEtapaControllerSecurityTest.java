package com.willyes.clemenintegra.produccion.web;

import com.willyes.clemenintegra.produccion.controller.ChecklistEtapaController;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChecklistEtapaController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class ChecklistEtapaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChecklistEtapaService checklistEtapaService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void bypassFilters() throws Exception {
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
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("Checklist etapa rechaza rol no autorizado")
    void obtenerChecklist_sinRol_devuelve403() throws Exception {
        mockMvc.perform(get("/api/produccion/etapas/{id}/checklist", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROD_ETAPA_CHECKLIST_READ")
    @DisplayName("Checklist etapa permite permiso canónico de lectura")
    void obtenerChecklist_conRolProduccion() throws Exception {
        when(checklistEtapaService.obtenerPorEtapa(anyLong())).thenReturn(ChecklistEtapaDTO.builder().etapaId(1L).build());

        mockMvc.perform(get("/api/produccion/etapas/{id}/checklist", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROD_ETAPA_CHECKLIST_READ")
    @DisplayName("Checklist etapa permite lectura con permiso canónico")
    void obtenerChecklist_conRolPlaneador() throws Exception {
        when(checklistEtapaService.obtenerPorEtapa(anyLong())).thenReturn(ChecklistEtapaDTO.builder().etapaId(2L).build());

        mockMvc.perform(get("/api/produccion/etapas/{id}/checklist", 2L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROD_ETAPA_CHECKLIST_WRITE")
    @DisplayName("Actualizar checklist permite permiso canónico de escritura")
    void actualizarChecklist_conPermisoEscritura() throws Exception {
        when(checklistEtapaService.actualizar(anyLong(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(ChecklistEtapaDTO.builder().etapaId(1L).build());

        mockMvc.perform(put("/api/produccion/etapas/{id}/checklist", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROD_ETAPA_CHECKLIST_READ")
    @DisplayName("Actualizar checklist rechaza rol planeador")
    void actualizarChecklist_conRolPlaneador_devuelve403() throws Exception {
        mockMvc.perform(put("/api/produccion/etapas/{id}/checklist", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }
}
