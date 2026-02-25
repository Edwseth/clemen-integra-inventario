package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceCancelarTest {

    @Mock private FormulaProductoRepository formulaProductoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SolicitudMovimientoService solicitudMovimientoService;
    @Mock private OrdenProduccionRepository ordenProduccionRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private CierreProduccionRepository cierreProduccionRepository;
    @Mock private MovimientoInventarioService movimientoInventarioService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private UnidadConversionService unidadConversionService;
    @Mock private EtapaProduccionRepository etapaProduccionRepository;
    @Mock private EtapaPlantillaRepository etapaPlantillaRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private UmValidator umValidator;
    @Mock private VidaUtilProductoRepository vidaUtilProductoRepository;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;
    @Mock private CosteoProduccionService costeoProduccionService;
    @Mock private RegularizacionTrazabilidadRepository regularizacionTrazabilidadRepository;

    @InjectMocks
    private OrdenProduccionServiceImpl service;


    @org.junit.jupiter.api.BeforeEach
    void defaultCosteo() {
        lenient().when(costeoProduccionService.calcularCostoUnitarioMaterialOp(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
    }

    @Test
    void cancelarOrden_actualizaEstadosYLiberaciones() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setEstado(EstadoProduccion.CREADA);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setEstado(EstadoSolicitudMovimiento.PENDIENTE);
        solicitud.setDetalles(new ArrayList<>(List.of(detalle)));
        detalle.setSolicitudMovimiento(solicitud);

        when(ordenProduccionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orden));
        when(reservaLoteService.existenReservasConsumidasPorOrden(1L)).thenReturn(false);
        when(solicitudMovimientoRepository.findWithDetalles(1L, null, null, null, false, List.of()))
                .thenReturn(List.of(solicitud));
        doNothing().when(reservaLoteService).liberarReservasPorOrden(anyLong());
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> {
            OrdenProduccion op = invocation.getArgument(0);
            if (op.getFechaFin() == null) {
                op.setFechaFin(LocalDateTime.now());
            }
            return op;
        });

        service.cancelarOrden(1L, "ajuste formula");

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.CANCELADA);
        assertThat(orden.getFechaFin()).isNotNull();
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.CANCELADO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudMovimiento.CANCELADA);
        verify(reservaLoteService, times(1)).liberarReservasPorOrden(1L);
    }

    @Test
    void cancelarOrden_rechazaCuandoHayReservasConsumidas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(5L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        when(ordenProduccionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(orden));
        when(reservaLoteService.existenReservasConsumidasPorOrden(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.cancelarOrden(5L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
    }

    @Test
    void cancelarOrden_rechazaEstadosNoCancelables() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(7L);
        orden.setEstado(EstadoProduccion.FINALIZADA);

        when(ordenProduccionRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> service.cancelarOrden(7L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
    }
}
