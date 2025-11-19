package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoInventarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimientoInventarioControllerSalidaPtTest {

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
        Mockito.reset(inventoryCatalogResolver, movimientoInventarioService, stockQueryService,
                productoRepository, loteProductoRepository, solicitudMovimientoRepository);
        when(inventoryCatalogResolver.isSalidaPtEnabled()).thenReturn(true);
        when(inventoryCatalogResolver.getTipoDetalleSalidaPtId()).thenReturn(9L);
        when(inventoryCatalogResolver.getTipoDetalleSalidaId()).thenReturn(9L);
        when(inventoryCatalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(1L);
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList())).thenReturn(Collections.emptyMap());
    }

    @Test
    @WithMockUser(username = "carlos", authorities = "ROL_JEFE_ALMACENES")
    void salidaProductoTerminadoSinFechaNoGeneraErrorDeFormato() throws Exception {
        MovimientoInventarioResponseDTO response = MovimientoInventarioResponseDTO.builder()
                .id(1L)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .clasificacion(ClasificacionMovimientoInventario.SALIDA_CLIENTE.name())
                .cantidad(BigDecimal.valueOf(50))
                .build();
        when(movimientoInventarioService.registrarMovimiento(any(MovimientoInventarioDTO.class)))
                .thenReturn(response);

        String payload = """
                {
                  \"tipoMovimiento\": \"SALIDA\",
                  \"clasificacionMovimientoInventario\": \"SALIDA_CLIENTE\",
                  \"productoId\": 123,
                  \"cantidad\": 50,
                  \"almacenOrigenId\": 3,
                  \"tipoMovimientoDetalleId\": 9,
                  \"autoSplit\": true,
                  \"docReferencia\": \"FA-2025-001\",
                  \"destinoTexto\": \"Cliente XYZ\",
                  \"atenciones\": []
                }
                """;

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture());
        assertThat(captor.getValue().fechaVencimiento()).as("fechaVencimiento debe ser opcional para salida PT")
                .isNull();
    }
}
