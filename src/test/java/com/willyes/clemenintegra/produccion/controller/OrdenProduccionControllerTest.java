package com.willyes.clemenintegra.produccion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import com.willyes.clemenintegra.produccion.dto.InsumoOPDTO;
import com.willyes.clemenintegra.produccion.dto.CancelarOrdenRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CrearOrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.produccion.service.ReporteOrdenProduccionService;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(controllers = OrdenProduccionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class OrdenProduccionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrdenProduccionService ordenProduccionService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ChecklistEtapaService checklistEtapaService;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @MockBean
    private ReporteOrdenProduccionService reporteOrdenProduccionService;

    @MockBean
    private OrdenProduccionRepository ordenProduccionRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    private CrearOrdenProduccionRequestDTO buildRequest() {
        LocalDateTime fin = LocalDateTime.now().plusHours(1);
        return CrearOrdenProduccionRequestDTO.builder()
                .fechaProgramada(fin)
                .cantidadProgramada(BigDecimal.TEN)
                .productoId(1L)
                .responsableId(2L)
                .build();
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes retorna 201 cuando la validación es correcta")
    void crearOrden_valida() throws Exception {
        OrdenProduccionResponseDTO orden = new OrdenProduccionResponseDTO();
        orden.id = 99L;
        orden.codigoOrden = "OP-123";

        ResultadoValidacionOrdenDTO respuesta = ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("ok")
                .orden(orden)
                .build();

        when(ordenProduccionService.crearOrden(any(CrearOrdenProduccionRequestDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esValida").value(true))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos responde 200")
    void listarMovimientosPorEtapa_respondeOk() throws Exception {
        when(ordenProduccionService.listarMovimientosPorEtapa(10L, 20L, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos", 10L, 20L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(ordenProduccionService).listarMovimientosPorEtapa(10L, 20L, null);
    }

    @Test
    @WithMockUser(authorities = "ROL_PLANEADOR")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/insumos permite ROL_PLANEADOR")
    void listarInsumos_planeador_respondeOk() throws Exception {
        when(ordenProduccionService.listarInsumos(10L)).thenReturn(List.of(new InsumoOPDTO()));

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/insumos", 10L))
                .andExpect(status().isOk());

        verify(ordenProduccionService).listarInsumos(10L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{id}/cancelar retorna 204 para cancelación exitosa")
    void cancelarOrden_respondeNoContent() throws Exception {
        CancelarOrdenRequestDTO request = new CancelarOrdenRequestDTO();
        request.setMotivo("Motivo de cancelación");

        doNothing().when(ordenProduccionService).cancelarOrden(10L, "Motivo de cancelación");

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cancelar", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(ordenProduccionService).cancelarOrden(10L, "Motivo de cancelación");
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{id}/cierres responde 200 y mapea categoriaProducto")
    void registrarCierre_recargaOrdenParaResponseConCategoria() throws Exception {
        CierreProduccionRequestDTO request = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("12.50"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        OrdenProduccion ordenServicio = OrdenProduccion.builder().id(10L).build();
        when(ordenProduccionService.registrarCierre(eq(10L), any(CierreProduccionRequestDTO.class)))
                .thenReturn(ordenServicio);

        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(6L)
                .nombre("Terminados")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build();
        UnidadMedida unidad = UnidadMedida.builder()
                .id(1L)
                .nombre("KILOGRAMO")
                .simbolo("KG")
                .build();
        Producto producto = Producto.builder()
                .id(99)
                .nombre("Jarabe")
                .categoriaProducto(categoria)
                .unidadMedida(unidad)
                .build();
        OrdenProduccion ordenRecargada = OrdenProduccion.builder()
                .id(10L)
                .codigoOrden("OP-010")
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(new BigDecimal("20.00"))
                .cantidadProducida(new BigDecimal("12.50"))
                .cantidadProducidaAcumulada(new BigDecimal("12.50"))
                .fechaInicio(LocalDateTime.now())
                .producto(producto)
                .unidadMedida(unidad)
                .build();
        when(ordenProduccionRepository.findByIdForCierreResponse(10L)).thenReturn(Optional.of(ordenRecargada));

        mockMvc.perform(post("/api/produccion/ordenes/{id}/cierres", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.categoriaProducto").value("PRODUCTO_TERMINADO"));

        verify(ordenProduccionService).registrarCierre(eq(10L), any(CierreProduccionRequestDTO.class));
        verify(ordenProduccionRepository).findByIdForCierreResponse(10L);
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("POST /api/produccion/ordenes permite SUPER_ADMIN")
    void crearOrden_superAdminPermitido() throws Exception {
        OrdenProduccionResponseDTO orden = new OrdenProduccionResponseDTO();
        orden.id = 101L;
        orden.codigoOrden = "OP-SA";

        ResultadoValidacionOrdenDTO respuesta = ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .mensaje("ok")
                .orden(orden)
                .build();

        when(ordenProduccionService.crearOrden(any(CrearOrdenProduccionRequestDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esValida").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes retorna 400 con code STOCK_INSUFICIENTE")
    void crearOrden_insuficiente() throws Exception {
        ResultadoValidacionOrdenDTO respuesta = ResultadoValidacionOrdenDTO.builder()
                .esValida(false)
                .mensaje("Stock insuficiente para algunos insumos")
                .unidadesMaximasProducibles(5)
                .insumosFaltantes(List.of(
                        InsumoFaltanteDTO.builder()
                                .productoId(10L)
                                .nombre("Extracto X")
                                .requerido(new BigDecimal("12.5"))
                                .disponible(new BigDecimal("8.0"))
                                .unidadSimbolo("kg")
                                .build()
                ))
                .build();

        when(ordenProduccionService.crearOrden(any(CrearOrdenProduccionRequestDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.esValida").value(false))
                .andExpect(jsonPath("$.code").value("STOCK_INSUFICIENTE"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes retorna 409 y code estable al requerir confirmación homeopática")
    void crearOrden_homeopaticoRequiereConfirmacion_responde409() throws Exception {
        when(ordenProduccionService.crearOrden(any(CrearOrdenProduccionRequestDTO.class)))
                .thenThrow(new CustomBusinessException(
                        ApiErrorCode.OP_HOMEOPATICO_REQUIERE_CONFIRMACION,
                        "Confirma para continuar",
                        java.util.Map.of("semanasVigencia", 78, "maxRecomendado", 30)
                ));

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OP_HOMEOPATICO_REQUIERE_CONFIRMACION"))
                .andExpect(jsonPath("$.details.semanasVigencia").value(78));
    }


    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes retorna 400 cuando confirmacionHomeopatico=true y motivo inválido")
    void crearOrden_homeopaticoConfirmadoConMotivoInvalido_responde400() throws Exception {
        CrearOrdenProduccionRequestDTO request = buildRequest();
        request.setConfirmacionHomeopatico(true);
        request.setMotivoOverrideHomeopatico("muy corto");

        mockMvc.perform(post("/api/produccion/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_CALIDAD")
    @DisplayName("GET /api/produccion/ordenes/{id} permite consulta a jefe de calidad")
    void obtenerOrden_jefeCalidadPuedeConsultar() throws Exception {
        com.willyes.clemenintegra.produccion.model.OrdenProduccion orden = com.willyes.clemenintegra.produccion.model.OrdenProduccion.builder()
                .id(15L)
                .codigoOrden("OP-15")
                .estado(com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion.CREADA)
                .build();

        when(ordenProduccionService.buscarPorId(15L)).thenReturn(Optional.of(orden));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/produccion/ordenes/{id}", 15L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/consumos responde 200 con lista vacía")
    void listarConsumosPorEtapa_retornaListaVacia() throws Exception {
        when(ordenProduccionService.listarConsumosPorEtapa(5L, 7L, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/consumos", 5L, 7L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(ordenProduccionService).listarConsumosPorEtapa(5L, 7L, null);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos responde 200 con lista vacía")
    void listarMovimientosPorEtapa_retornaListaVacia() throws Exception {
        when(ordenProduccionService.listarMovimientosPorEtapa(8L, 9L, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos", 8L, 9L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(ordenProduccionService).listarMovimientosPorEtapa(8L, 9L, null);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos responde con movimientos")
    void listarMovimientosPorEtapa_retornaMovimientos() throws Exception {
        MovimientoInventarioResponseDTO movimiento = MovimientoInventarioResponseDTO.builder()
                .id(77L)
                .ordenProduccionId(8L)
                .ordenProduccionEtapaId(9L)
                .clasificacion("SALIDA_PRODUCCION")
                .build();
        when(ordenProduccionService.listarMovimientosPorEtapa(8L, 9L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(List.of(movimiento));

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/movimientos", 8L, 9L)
                        .param("clasificacion", "SALIDA_PRODUCCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(77L))
                .andExpect(jsonPath("$[0].ordenProduccionEtapaId").value(9L));

        verify(ordenProduccionService).listarMovimientosPorEtapa(8L, 9L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist responde 200 y retorna lista vacía")
    void obtenerChecklistPorEtapa_respondeOk() throws Exception {
        ChecklistEtapaDTO checklist = ChecklistEtapaDTO.builder()
                .etapaId(12L)
                .ordenProduccionId(11L)
                .items(List.of())
                .faltantesObligatorios(0)
                .completo(false)
                .build();
        when(checklistEtapaService.obtenerPorOrdenYEtapa(11L, 12L)).thenReturn(checklist);

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", 11L, 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(checklistEtapaService).obtenerPorOrdenYEtapa(11L, 12L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist responde 200 con checklist lleno")
    void obtenerChecklistPorEtapa_conDatos() throws Exception {
        ChecklistItemDTO item = ChecklistItemDTO.builder()
                .id(21L)
                .nombrePaso("Validar equipo")
                .obligatorio(true)
                .completado(true)
                .observacion("ok")
                .build();
        ChecklistEtapaDTO checklist = ChecklistEtapaDTO.builder()
                .etapaId(14L)
                .ordenProduccionId(13L)
                .items(List.of(item))
                .completo(true)
                .faltantesObligatorios(0)
                .build();
        when(checklistEtapaService.obtenerPorOrdenYEtapa(13L, 14L)).thenReturn(checklist);

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", 13L, 14L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(21L))
                .andExpect(jsonPath("$.completo").value(true));

        verify(checklistEtapaService).obtenerPorOrdenYEtapa(13L, 14L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist devuelve 400 si la etapa no corresponde")
    void obtenerChecklistPorEtapa_etapaNoPertenece() throws Exception {
        when(checklistEtapaService.obtenerPorOrdenYEtapa(9L, 99L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "ETAPA_NO_PERTENECE_A_ORDEN"));

        mockMvc.perform(get("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", 9L, 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()));

        verify(checklistEtapaService).obtenerPorOrdenYEtapa(9L, 99L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist responde 200")
    void actualizarChecklistPorEtapa_respondeOk() throws Exception {
        ChecklistEtapaDTO checklist = ChecklistEtapaDTO.builder()
                .etapaId(14L)
                .ordenProduccionId(13L)
                .items(List.of())
                .completo(true)
                .faltantesObligatorios(0)
                .build();
        when(checklistEtapaService.actualizarEnOrden(eq(13L), eq(14L), any())).thenReturn(checklist);

        mockMvc.perform(post("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/checklist", 13L, 14L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());

        verify(checklistEtapaService).actualizarEnOrden(eq(13L), eq(14L), any());
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar responde 200 y retorna la orden")
    void iniciarEtapa_postDevuelveOrden() throws Exception {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(55L)
                .codigoOrden("OP-55")
                .estado(EstadoProduccion.EN_PROCESO)
                .cantidadProgramada(BigDecimal.TEN)
                .cantidadProducida(BigDecimal.ZERO)
                .cantidadProducidaAcumulada(BigDecimal.ZERO)
                .fechaInicio(LocalDateTime.now())
                .build();
        when(ordenProduccionService.iniciarEtapa(55L, 7L)).thenReturn(new EtapaProduccion());
        when(ordenProduccionService.buscarPorId(55L)).thenReturn(Optional.of(orden));

        mockMvc.perform(post("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar", 55L, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55L))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));

        verify(ordenProduccionService).iniciarEtapa(55L, 7L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar devuelve 409 si existe otra etapa activa")
    void iniciarEtapa_postConflictoOtraActiva() throws Exception {
        when(ordenProduccionService.iniciarEtapa(77L, 88L))
                .thenThrow(new CustomBusinessException(ApiErrorCode.PRODUCCION_OTRA_ETAPA_ACTIVA, "Ya existe una etapa activa"));

        mockMvc.perform(post("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/iniciar", 77L, 88L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.PRODUCCION_OTRA_ETAPA_ACTIVA.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("POST /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/finalizar responde 200")
    void finalizarEtapa_postRespondeOk() throws Exception {
        Usuario usuario = Usuario.builder().id(10L).nombreCompleto("Operador").build();
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(22L)
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(11L).build())
                .build();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(ordenProduccionService.finalizarEtapa(11L, 22L, 10L)).thenReturn(etapa);

        mockMvc.perform(post("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/finalizar", 11L, 22L))
                .andExpect(status().isOk());

        verify(ordenProduccionService).finalizarEtapa(11L, 22L, 10L);
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("PATCH /api/produccion/ordenes/{ordenId}/etapas/{etapaId}/finalizar responde 200")
    void finalizarEtapa_patchRespondeOk() throws Exception {
        Usuario usuario = Usuario.builder().id(20L).nombreCompleto("Operador Patch").build();
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(33L)
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(44L).build())
                .build();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(ordenProduccionService.finalizarEtapa(44L, 33L, 20L)).thenReturn(etapa);

        mockMvc.perform(patch("/api/produccion/ordenes/{ordenId}/etapas/{etapaId}/finalizar", 44L, 33L))
                .andExpect(status().isOk());

        verify(ordenProduccionService).finalizarEtapa(44L, 33L, 20L);
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    @DisplayName("GET /api/produccion/ordenes/lookup permite lookup por codigo a ROL_CONTADOR")
    void lookupPorCodigo_contador_respondeOk() throws Exception {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(10L)
                .codigoOrden("OP-CLEMEN-20260214-01")
                .estado(EstadoProduccion.CREADA)
                .build();
        when(ordenProduccionRepository.findByCodigoOrdenIgnoreCase("OP-CLEMEN-20260214-01"))
                .thenReturn(Optional.of(orden));

        mockMvc.perform(get("/api/produccion/ordenes/lookup")
                        .param("codigo", "OP-CLEMEN-20260214-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.codigoOrden").value("OP-CLEMEN-20260214-01"));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("GET /api/produccion/ordenes/lookup permite lookup por codigo a ROL_SUPER_ADMIN")
    void lookupPorCodigo_superAdmin_respondeOk() throws Exception {
        OrdenProduccion orden = OrdenProduccion.builder()
                .id(11L)
                .codigoOrden("OP-CLEMEN-20260214-02")
                .estado(EstadoProduccion.CREADA)
                .build();
        when(ordenProduccionRepository.findByCodigoOrdenIgnoreCase("OP-CLEMEN-20260214-02"))
                .thenReturn(Optional.of(orden));

        mockMvc.perform(get("/api/produccion/ordenes/lookup")
                        .param("codigo", "OP-CLEMEN-20260214-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.codigoOrden").value("OP-CLEMEN-20260214-02"));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_PRODUCCION")
    @DisplayName("GET /api/produccion/ordenes/lookup rechaza roles no autorizados")
    void lookupPorCodigo_rolNoAutorizado_responde403() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/lookup")
                        .param("codigo", "OP-CLEMEN-20260214-03"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("GET /api/produccion/ordenes/lookup devuelve 404 cuando no existe la orden")
    void lookupPorCodigo_noExiste_responde404() throws Exception {
        when(ordenProduccionRepository.findByCodigoOrdenIgnoreCase("OP-CLEMEN-404"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produccion/ordenes/lookup")
                        .param("codigo", "OP-CLEMEN-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_SUPER_ADMIN")
    @DisplayName("GET /api/produccion/ordenes/lookup devuelve 400 cuando no se envian parametros")
    void lookupSinParametros_responde400() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/lookup"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()));
    }


    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    @DisplayName("GET /api/produccion/ordenes/autocomplete retorna 200 con resultados")
    void autocomplete_conCodigoValido_responde200() throws Exception {
        OrdenProduccionResponseDTO dto = new OrdenProduccionResponseDTO();
        dto.id = 10L;
        dto.codigoOrden = "OP-CLEMEN-20260212-05";

        when(ordenProduccionService.listarPaginado(eq("OP-"), eq(null), eq(null), eq(null), eq(null), eq(null), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/produccion/ordenes/autocomplete")
                        .param("codigo", "OP-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L))
                .andExpect(jsonPath("$.content[0].codigoOrden").value("OP-CLEMEN-20260212-05"));
    }

    @Test
    @WithMockUser(authorities = "ROL_CONTADOR")
    @DisplayName("GET /api/produccion/ordenes/autocomplete retorna 400 cuando codigo tiene menos de 2 caracteres")
    void autocomplete_conCodigoCorto_responde400() throws Exception {
        mockMvc.perform(get("/api/produccion/ordenes/autocomplete")
                        .param("codigo", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.SOLICITUD_INVALIDA.name()));
    }

}
