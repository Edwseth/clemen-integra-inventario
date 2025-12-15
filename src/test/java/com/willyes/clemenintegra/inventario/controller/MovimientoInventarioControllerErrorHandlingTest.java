package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        when(movimientoInventarioService.registrarMovimiento(any()))
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
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }
}
