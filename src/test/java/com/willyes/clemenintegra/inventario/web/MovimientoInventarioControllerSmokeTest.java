package com.willyes.clemenintegra.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willyes.clemenintegra.inventario.controller.MovimientoInventarioController;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter;
import com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"DB_SECURPASS=dummy", "DB_SECURNAME=dummy"})
class MovimientoInventarioControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @MockBean
    private ProductoRepository productoRepository;

    @MockBean
    private LoteProductoRepository loteProductoRepository;

    @MockBean
    private SolicitudMovimientoRepository solicitudMovimientoRepository;

    @MockBean
    private StockQueryService stockQueryService;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UsuarioInactivoFilter usuarioInactivoFilter;

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("POST /api/movimientos registra movimiento de entrada y devuelve 201")
    void registrarMovimientoEntrada_deberiaRetornar201() throws Exception {
        Map<String, Object> payload = Map.of(
                "cantidad", 5,
                "productoId", 1,
                "tipoMovimiento", TipoMovimiento.ENTRADA.name()
        );

        MovimientoInventarioResponseDTO response = MovimientoInventarioResponseDTO.builder()
                .id(200L)
                .productoId(1L)
                .nombreProducto("Producto demo")
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .fechaIngreso(LocalDateTime.now())
                .build();

        when(movimientoInventarioService.registrarMovimiento(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-" + UUID.randomUUID())
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.productoId").value(1))
                .andExpect(jsonPath("$.tipoMovimiento").value(TipoMovimiento.ENTRADA.name()));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("POST /api/movimientos con datos inválidos devuelve 400 con estructura de error")
    void registrarMovimiento_conBodyInvalido_deberiaRetornar400() throws Exception {
        Map<String, Object> payload = Map.of(
                "tipoMovimiento", TipoMovimiento.ENTRADA.name(),
                "cantidad", BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.message").value("Solicitud inválida"))
                .andExpect(jsonPath("$.details").isArray());
    }


    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("POST /api/movimientos acepta fechaVencimiento ISO yyyy-MM-dd en devoluciones PT")
    void registrarMovimientoDevolucionPt_conFechaIso_deberiaNormalizarInicioDelDia() throws Exception {
        Map<String, Object> payload = Map.of(
                "cantidad", 10,
                "productoId", 1,
                "tipoMovimiento", "RECEPCION",
                "clasificacionMovimientoInventario", "RECEPCION_DEVOLUCION_CLIENTE",
                "fechaVencimiento", "2026-09-02"
        );

        MovimientoInventarioResponseDTO response = MovimientoInventarioResponseDTO.builder()
                .id(201L)
                .productoId(1L)
                .tipoMovimiento(TipoMovimiento.RECEPCION)
                .fechaIngreso(LocalDateTime.now())
                .build();

        when(movimientoInventarioService.registrarMovimiento(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-" + UUID.randomUUID())
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        ArgumentCaptor<com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO> captor =
                ArgumentCaptor.forClass(com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().fechaVencimiento())
                .isEqualTo(LocalDateTime.of(2026, Month.SEPTEMBER, 2, 0, 0));
    }

    @Test
    @WithMockUser(authorities = "ROL_JEFE_ALMACENES")
    @DisplayName("GET /api/movimientos devuelve 200 con página mínima")
    void listarMovimientos_deberiaRetornar200() throws Exception {
        MovimientoInventarioResponseDTO movimiento = MovimientoInventarioResponseDTO.builder()
                .id(1L)
                .productoId(1L)
                .nombreProducto("Producto demo")
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .cantidad(BigDecimal.ONE)
                .build();
        Pageable pageable = PageRequest.of(0, 5);
        Page<MovimientoInventarioResponseDTO> page = new PageImpl<>(List.of(movimiento), pageable, 1);

        when(movimientoInventarioService.listarTodos(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/movimientos")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tipoMovimiento").value(TipoMovimiento.ENTRADA.name()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
