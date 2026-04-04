package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.CorridaOrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private OrdenProduccionService ordenProduccionService;
    @MockBean
    private OrdenProduccionRepository ordenProduccionRepository;
    @MockBean
    private VidaUtilProductoRepository vidaUtilProductoRepository;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_WRITE")
    void crearPlanPermiteWritePlanSemanal() throws Exception {
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
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_WRITE")
    void generarOrdenDesdeDetallePermiteWritePlanSemanal() throws Exception {
        ResultadoValidacionOrdenDTO response = ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("ok")
                .build();
        when(ordenProduccionService.crearOrdenDesdePlanSemanal(eq(10L), eq(20L), any()))
                .thenReturn(response);

        String payload = """
                {
                  "productoId": 99,
                  "cantidadProgramada": 10,
                  "fechaProgramada": "2030-01-01T10:00:00",
                  "responsableId": 7
                }
                """;

        mockMvc.perform(post("/api/planeacion/planes-semanales/10/detalles/20/generar-op")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esValida").value(true));
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_WRITE")
    void generarCorridaDesdeDetallePermiteWritePlanSemanal() throws Exception {
        CorridaOrdenProduccionResponseDTO response = CorridaOrdenProduccionResponseDTO.builder()
                .planId(10L)
                .planDetalleId(20L)
                .totalOpCreadas(2)
                .build();
        when(ordenProduccionService.ejecutarCorridaHomeopaticaDesdePlanSemanal(eq(10L), eq(20L), any()))
                .thenReturn(response);

        String payload = """
                {
                  "responsableId": 7,
                  "fechaProgramada": "2030-01-01T10:00:00",
                  "idempotencyKey": "idem-1"
                }
                """;

        mockMvc.perform(post("/api/planeacion/planes-semanales/10/detalles/20/generar-op-corrida")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalOpCreadas").value(2));
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void listarPlanPermiteReadPlanSemanal() throws Exception {
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
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
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
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void listarPlanPermitePermisoReadPlanSemanal() throws Exception {
        when(planProduccionService.listar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/planeacion/planes-semanales"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PO_READ")
    void crearPlanRechazaSiSoloTieneLecturaCanonica() throws Exception {
        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .content("{}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_COMPRADOR")
    void crearPlanRechazaComprador() throws Exception {
        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .content("{}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void obtenerDetalleIncluyeProductoYUnidad() throws Exception {
        PlanProduccionDetalle detalle = PlanProduccionDetalle.builder()
                .id(1L)
                .producto(Producto.builder()
                        .id(807)
                        .codigoSku("SKU-001")
                        .nombre("Producto Terminado")
                        .unidadMedida(UnidadMedida.builder()
                                .id(5L)
                                .nombre("Kilogramo")
                                .simbolo("KG")
                                .build())
                        .build())
                .cantidadPlanificada(BigDecimal.valueOf(2500))
                .unidadMedida(UnidadMedida.builder()
                        .id(5L)
                        .nombre("Kilogramo")
                        .simbolo("KG")
                        .build())
                .prioridad(1)
                .origenDemanda("Demanda")
                .observacion("Obs")
                .build();

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(5L)
                .semanaInicio(LocalDate.of(2025, 11, 30))
                .semanaFin(LocalDate.of(2025, 12, 6))
                .estado(EstadoPlanProduccion.BORRADOR)
                .detalles(List.of(detalle))
                .build();

        when(planProduccionService.buscarPorId(5L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(Set.of(1L))).thenReturn(List.of());
        when(vidaUtilProductoRepository.findAllById(Set.of(807))).thenReturn(List.of());

        mockMvc.perform(get("/api/planeacion/planes-semanales/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].productoId").value(807))
                .andExpect(jsonPath("$.detalles[0].producto.id").value(807))
                .andExpect(jsonPath("$.detalles[0].producto.codigoSku").value("SKU-001"))
                .andExpect(jsonPath("$.detalles[0].producto.nombre").value("Producto Terminado"))
                .andExpect(jsonPath("$.detalles[0].producto.unidadMedida").value("KG"))
                .andExpect(jsonPath("$.detalles[0].unidadMedidaId").value(5))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.id").value(5))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.nombre").value("Kilogramo"))
                .andExpect(jsonPath("$.detalles[0].unidadMedida.simbolo").value("KG"))
                .andExpect(jsonPath("$.detalles[0].tipoProducto").value("PT"))
                .andExpect(jsonPath("$.detalles[0].esHomeopatico").value(false))
                .andExpect(jsonPath("$.detalles[0].totalOpAsociadas").value(0))
                .andExpect(jsonPath("$.detalles[0].cantidadTotalProgramadaEnOp").value(0))
                .andExpect(jsonPath("$.detalles[0].cantidadPendiente").value(2500))
                .andExpect(jsonPath("$.detalles[0].modoAccionSugerido").value("UNICA"));
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void obtenerPlanPermiteReadPlanSemanal() throws Exception {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(10L)
                .semanaInicio(LocalDate.of(2024, 6, 10))
                .semanaFin(LocalDate.of(2024, 6, 16))
                .detalles(List.of())
                .build();

        when(planProduccionService.buscarPorId(10L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(any())).thenReturn(List.of());
        when(vidaUtilProductoRepository.findAllById(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/planeacion/planes-semanales/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }


    @Test
    @WithMockUser(authorities = "PO_MRP_READ")
    void rechazaUsuarioSinPermisosReadNiWrite() throws Exception {
        mockMvc.perform(get("/api/planeacion/planes-semanales"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/planeacion/planes-semanales")
                        .content("{}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void confirmarPlanRechazaSiSoloTieneRead() throws Exception {
        mockMvc.perform(post("/api/planeacion/planes-semanales/99/confirmar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PO_PLAN_SEMANAL_READ")
    void modoAccionSugeridoCubreCasosPtPsYHomeopatico() throws Exception {
        PlanProduccionDetalle ptNoHomeoSinOp = detalle(11L, 801, TipoCategoria.PRODUCTO_TERMINADO, BigDecimal.valueOf(50));
        PlanProduccionDetalle ptNoHomeoConOp = detalle(12L, 802, TipoCategoria.PRODUCTO_TERMINADO, BigDecimal.valueOf(50));
        PlanProduccionDetalle psSinOp = detalle(13L, 803, TipoCategoria.PRODUCTO_SEMI_ELABORADO, BigDecimal.valueOf(50));
        PlanProduccionDetalle psConOp = detalle(14L, 804, TipoCategoria.PRODUCTO_SEMI_ELABORADO, BigDecimal.valueOf(50));
        PlanProduccionDetalle ptHomeoSinOp = detalle(15L, 805, TipoCategoria.PRODUCTO_TERMINADO, BigDecimal.valueOf(50));
        PlanProduccionDetalle ptHomeoParcial = detalle(16L, 806, TipoCategoria.PRODUCTO_TERMINADO, BigDecimal.valueOf(80));
        PlanProduccionDetalle ptHomeoCompleto = detalle(17L, 807, TipoCategoria.PRODUCTO_TERMINADO, BigDecimal.valueOf(60));

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(99L)
                .semanaInicio(LocalDate.of(2026, 1, 5))
                .semanaFin(LocalDate.of(2026, 1, 11))
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .detalles(List.of(ptNoHomeoSinOp, ptNoHomeoConOp, psSinOp, psConOp, ptHomeoSinOp, ptHomeoParcial, ptHomeoCompleto))
                .build();

        when(planProduccionService.buscarPorId(99L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(Set.of(11L, 12L, 13L, 14L, 15L, 16L, 17L)))
                .thenReturn(List.of(
                        op(1001L, 12L, "OP-12", BigDecimal.valueOf(10)),
                        op(1002L, 14L, "OP-14", BigDecimal.valueOf(15)),
                        op(1003L, 16L, "OP-16-A", BigDecimal.valueOf(30)),
                        op(1004L, 16L, "OP-16-B", BigDecimal.valueOf(20)),
                        op(1005L, 17L, "OP-17-A", BigDecimal.valueOf(30)),
                        op(1006L, 17L, "OP-17-B", BigDecimal.valueOf(30))
                ));
        when(vidaUtilProductoRepository.findAllById(Set.of(801, 802, 803, 804, 805, 806, 807)))
                .thenReturn(List.of(
                        vidaUtil(805, 78),
                        vidaUtil(806, 78),
                        vidaUtil(807, 78),
                        vidaUtil(801, 52),
                        vidaUtil(802, 52)
                ));

        mockMvc.perform(get("/api/planeacion/planes-semanales/99"))
                .andExpect(status().isOk())
                // PT no homeopático sin OP => UNICA
                .andExpect(jsonPath("$.detalles[0].modoAccionSugerido").value("UNICA"))
                // PT no homeopático con OP => BLOQUEADA
                .andExpect(jsonPath("$.detalles[1].modoAccionSugerido").value("BLOQUEADA"))
                // PS sin OP => UNICA
                .andExpect(jsonPath("$.detalles[2].modoAccionSugerido").value("UNICA"))
                // PS con OP => BLOQUEADA
                .andExpect(jsonPath("$.detalles[3].modoAccionSugerido").value("BLOQUEADA"))
                // PT homeopático sin OP => CORRIDA
                .andExpect(jsonPath("$.detalles[4].modoAccionSugerido").value("CORRIDA"))
                // PT homeopático parcial => CORRIDA
                .andExpect(jsonPath("$.detalles[5].modoAccionSugerido").value("CORRIDA"))
                .andExpect(jsonPath("$.detalles[5].cantidadPendiente").value(30))
                // PT homeopático completo => BLOQUEADA
                .andExpect(jsonPath("$.detalles[6].modoAccionSugerido").value("BLOQUEADA"))
                .andExpect(jsonPath("$.detalles[6].cantidadPendiente").value(0));
    }

    private PlanProduccionDetalle detalle(Long detalleId, Integer productoId, TipoCategoria tipo, BigDecimal cantidadPlanificada) {
        return PlanProduccionDetalle.builder()
                .id(detalleId)
                .producto(Producto.builder()
                        .id(productoId)
                        .codigoSku("SKU-" + productoId)
                        .nombre("Prod-" + productoId)
                        .categoriaProducto(CategoriaProducto.builder().tipo(tipo).build())
                        .unidadMedida(UnidadMedida.builder().id(1L).nombre("Unidad").simbolo("UND").build())
                        .build())
                .cantidadPlanificada(cantidadPlanificada)
                .unidadMedida(UnidadMedida.builder().id(1L).nombre("Unidad").simbolo("UND").build())
                .build();
    }

    private OrdenProduccion op(Long id, Long detalleId, String codigo, BigDecimal cantidad) {
        return OrdenProduccion.builder()
                .id(id)
                .codigoOrden(codigo)
                .cantidadProgramada(cantidad)
                .planProduccionDetalle(PlanProduccionDetalle.builder().id(detalleId).build())
                .build();
    }

    private VidaUtilProducto vidaUtil(Integer productoId, Integer semanas) {
        return VidaUtilProducto.builder()
                .productoId(productoId)
                .semanasVigencia(semanas)
                .build();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
