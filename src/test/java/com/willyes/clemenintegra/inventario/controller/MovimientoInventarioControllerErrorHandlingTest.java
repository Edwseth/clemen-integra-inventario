package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimientoInventarioControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovimientoInventarioService movimientoInventarioService;

    @MockBean
    private InventoryCatalogResolver inventoryCatalogResolver;

    @MockBean
    private StockQueryService stockQueryService;

    @MockBean
    private com.willyes.clemenintegra.inventario.repository.ProductoRepository productoRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.repository.LoteProductoRepository loteProductoRepository;

    @MockBean
    private com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository solicitudMovimientoRepository;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.UsuarioInactivoFilter usuarioInactivoFilter;

    @MockBean
    private com.willyes.clemenintegra.shared.security.JwtAuthenticationProvider jwtAuthenticationProvider;

    @BeforeEach
    void setUpMocks() {
        Mockito.reset(movimientoInventarioService, inventoryCatalogResolver, stockQueryService,
                productoRepository, loteProductoRepository, solicitudMovimientoRepository);
    }

    @Test
    @WithMockUser(username = "analista", authorities = "ROL_ALMACENISTA")
    void cuandoServicioLanzaResponseStatusExceptionSePropaga() throws Exception {
        when(movimientoInventarioService.registrarMovimiento(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "LOTE_NO_DISPONIBLE_TRANSFERIR"));

        String payload = """
                {
                  \"tipoMovimiento\": \"TRANSFERENCIA\",
                  \"clasificacionMovimientoInventario\": \"TRANSFERENCIA_GENERAL\",
                  \"productoId\": 8,
                  \"cantidad\": 1000,
                  \"almacenOrigenId\": 1,
                  \"almacenDestinoId\": 6,
                  \"tipoMovimientoDetalleId\": 5
                }
                """;

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-" + UUID.randomUUID())
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "analista", authorities = "ROL_ALMACENISTA")
    void cuandoLoteNoLiberadoRetorna422ConCodigo() throws Exception {
        when(movimientoInventarioService.registrarMovimiento(any(), anyString()))
                .thenThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                        "El lote aún no está liberado por Calidad y no puede utilizarse en esta operación."));

        String payload = """
                {
                  \"tipoMovimiento\": \"TRANSFERENCIA\",
                  \"clasificacionMovimientoInventario\": \"TRANSFERENCIA_GENERAL\",
                  \"productoId\": 8,
                  \"cantidad\": 10,
                  \"almacenOrigenId\": 1,
                  \"almacenDestinoId\": 6
                }
                """;

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-" + UUID.randomUUID())
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO.getCode()))
                .andExpect(jsonPath("$.message").value("El lote aún no está liberado por Calidad y no puede utilizarse en esta operación."));
    }

    @Test
    @WithMockUser(username = "analista", authorities = "ROL_ALMACENISTA")
    void transferenciaLiberadaDevuelveCreated() throws Exception {
        when(movimientoInventarioService.registrarMovimiento(any(), anyString()))
                .thenReturn(com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO.builder()
                        .id(42L)
                        .build());

        String payload = """
                {
                  \"tipoMovimiento\": \"TRANSFERENCIA\",
                  \"clasificacionMovimientoInventario\": \"TRANSFERENCIA_GENERAL\",
                  \"productoId\": 12,
                  \"cantidad\": 5,
                  \"almacenOrigenId\": 2,
                  \"almacenDestinoId\": 3
                }
                """;

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-" + UUID.randomUUID())
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42));
    }
}
