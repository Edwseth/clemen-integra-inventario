package com.willyes.clemenintegra.produccion.web;

import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.controller.OrdenProduccionController;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.produccion.service.ReporteOrdenProduccionService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.SecurityConfig;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdenProduccionController.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
@Import(SecurityConfig.class)
class OrdenProduccionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenProduccionService ordenProduccionService;
    @MockBean
    private ReporteOrdenProduccionService reporteOrdenProduccionService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private ChecklistEtapaService checklistEtapaService;
    @MockBean
    private MovimientoInventarioService movimientoInventarioService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;
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
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("GET /api/produccion/ordenes/{id} sin rol permitido devuelve 403")
    void obtenerOrden_sinPermisos_devuelve403() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("GET /api/produccion/ordenes permite consulta a jefe de calidad")
    void listarOrdenes_conRolJefeCalidad_devuelve200() throws Exception {
        when(ordenProduccionService.listarPaginado(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/produccion/ordenes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("GET /api/produccion/ordenes/{id} permite ver detalle con rol de calidad")
    void obtenerOrden_conRolJefeCalidad_devuelve404SiNoExiste() throws Exception {
        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("POST /api/produccion/ordenes es rechazado para jefe de calidad")
    void crearOrden_conRolJefeCalidad_devuelve403() throws Exception {
        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("POST /api/produccion/ordenes/{id}/cierres es rechazado para jefe de calidad")
    void registrarCierre_conRolJefeCalidad_devuelve403() throws Exception {
        mockMvc.perform(post("/api/produccion/ordenes/{id}/cierres", 5L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    @DisplayName("GET /api/produccion/ordenes permite listar con rol planeador")
    void listarOrdenes_conRolPlaneador_devuelve200() throws Exception {
        when(ordenProduccionService.listarPaginado(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/produccion/ordenes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    @DisplayName("GET /api/produccion/ordenes/{id} permite ver detalle con rol planeador")
    void obtenerOrden_conRolPlaneador_devuelve404SiNoExiste() throws Exception {
        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROL_PLANEADOR", "PROD_OP_CREATE"})
    @DisplayName("POST /api/produccion/ordenes permite crear con permiso de creación")
    void crearOrden_conRolPlaneador_devuelve201() throws Exception {
        when(ordenProduccionService.crearOrden(any()))
                .thenReturn(ResultadoValidacionOrdenDTO.builder().esValida(true).build());

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    @DisplayName("PUT /api/produccion/ordenes/{id} permite actualizar con rol planeador")
    void actualizarOrden_conRolPlaneador_devuelve404SiNoExiste() throws Exception {
        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/produccion/ordenes/{id}", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    @DisplayName("POST /api/produccion/ordenes/{id}/cancelar permite workflow con rol planeador")
    void cancelarOrden_conRolPlaneador_devuelve204() throws Exception {
        doNothing().when(ordenProduccionService).cancelarOrden(any(), any());

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cancelar", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("POST /api/produccion/ordenes rechaza rol no autorizado")
    void crearOrden_conRolAlmacenista_devuelve403() throws Exception {
        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROL_PLANEADOR", "PROD_ETAPA_START"})
    @DisplayName("PATCH /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar permite iniciar etapa")
    void iniciarEtapa_conPermisoPlaneador_devuelve200() throws Exception {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setEstado(EstadoProduccion.CREADA);

        when(ordenProduccionService.buscarPorId(1L)).thenReturn(Optional.of(orden));
        when(ordenProduccionService.iniciarEtapa(1L, 2L)).thenReturn(new com.willyes.clemenintegra.produccion.model.EtapaProduccion());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar", 1L, 2L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_ALMACENISTA")
    @DisplayName("POST /api/produccion/ordenes/{id}/cancelar rechaza rol no autorizado")
    void cancelarOrden_conRolAlmacenista_devuelve403() throws Exception {
        mockMvc.perform(post("/api/produccion/ordenes/{id}/cancelar", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/produccion/ordenes/{id} sin autenticación devuelve 401")
    void obtenerOrden_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
