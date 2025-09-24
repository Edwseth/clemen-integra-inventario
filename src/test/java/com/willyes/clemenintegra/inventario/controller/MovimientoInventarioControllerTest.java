package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioControllerTest {

    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private InventoryCatalogResolver inventoryCatalogResolver;

    @InjectMocks
    private MovimientoInventarioController controller;

    private Producto producto;
    private LoteProducto lote;
    private SolicitudMovimiento solicitud;
    private static final long PRE_BODEGA_ID = 999L;

    @BeforeEach
    void setUp() {
        when(inventoryCatalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(PRE_BODEGA_ID);

        producto = new Producto();
        producto.setId(1);

        lote = new LoteProducto();
        lote.setId(10L);
        lote.setStockLote(BigDecimal.valueOf(3));
        lote.setStockReservado(BigDecimal.valueOf(4));

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(100L)
                .lote(lote)
                .cantidad(BigDecimal.valueOf(4))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .build();

        solicitud = SolicitudMovimiento.builder()
                .id(50L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .detalles(List.of(detalle))
                .build();
        detalle.setSolicitudMovimiento(solicitud);
    }

    @Test
    void registrarSalidaConReservaAutorizadaDebeResponderCreated() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(4),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                producto.getId(),
                lote.getId(),
                100,
                null,
                null,
                null,
                null,
                null,
                solicitud.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(productoRepository.findById(producto.getId().longValue())).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(solicitudMovimientoRepository.findWithDetalles(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList()))
                .thenReturn(Map.of(producto.getId().longValue(), BigDecimal.ONE));
        when(movimientoInventarioService.registrarMovimiento(any(MovimientoInventarioDTO.class)))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(200L).build());

        ResponseEntity<?> response = controller.registrar(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(movimientoInventarioService).registrarMovimiento(dto);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSolicitudMovimiento.class, names = {"PENDIENTE", "RESERVADA"})
    void registrarSalidaConReservaPendienteDebeDelegarAlServicio(EstadoSolicitudMovimiento estado) {
        solicitud.setEstado(estado);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(4),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                producto.getId(),
                lote.getId(),
                100,
                null,
                null,
                null,
                null,
                null,
                solicitud.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(productoRepository.findById(producto.getId().longValue())).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(solicitudMovimientoRepository.findWithDetalles(solicitud.getId())).thenReturn(Optional.of(solicitud));
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList()))
                .thenReturn(Map.of(producto.getId().longValue(), BigDecimal.ONE));
        when(movimientoInventarioService.registrarMovimiento(any(MovimientoInventarioDTO.class)))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(201L).build());

        ResponseEntity<?> response = controller.registrar(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(movimientoInventarioService).registrarMovimiento(dto);
    }

    @Test
    void registrarSalidaConStockSoloEnPreBodegaDebeResponderConflict() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                BigDecimal.valueOf(2),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                producto.getId(),
                lote.getId(),
                (int) PRE_BODEGA_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(productoRepository.findById(producto.getId().longValue())).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(lote.getId())).thenReturn(Optional.of(lote));

        ResponseEntity<?> response = controller.registrar(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response.getBody()).get("message"))
                .isEqualTo("No hay suficiente stock disponible");
        verify(movimientoInventarioService, never()).registrarMovimiento(any(MovimientoInventarioDTO.class));
    }
}

