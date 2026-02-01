package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoItemDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudesPorOrdenDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudMovimientoServiceImplPorOrdenTest {

    @Mock
    private SolicitudMovimientoRepository repository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;

    @InjectMocks
    private SolicitudMovimientoServiceImpl service;

    @Test
    void obtenerPorOrdenMantieneOrigenDesdeLoteYEstadoPendienteEnDetalle() {
        Almacen origen = Almacen.builder()
                .id(1)
                .nombre("Alm Origen")
                .ubicacion("OR-01")
                .build();
        Almacen destino = Almacen.builder()
                .id(6)
                .nombre("Pre-Bodega Producción")
                .ubicacion("PB-01")
                .build();
        LoteProducto lote = LoteProducto.builder()
                .id(10L)
                .codigoLote("L-001")
                .almacen(origen)
                .build();
        SolicitudMovimientoDetalle pendienteDetalle = SolicitudMovimientoDetalle.builder()
                .id(228L)
                .lote(lote)
                .cantidad(BigDecimal.TEN)
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(null)
                .almacenDestino(destino)
                .build();
        SolicitudMovimientoDetalle atendidoDetalle = SolicitudMovimientoDetalle.builder()
                .id(229L)
                .lote(lote)
                .cantidad(BigDecimal.ONE)
                .cantidadAtendida(BigDecimal.ONE)
                .estado(EstadoSolicitudMovimientoDetalle.ATENDIDO)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .build();
        OrdenProduccion op = OrdenProduccion.builder()
                .id(100L)
                .codigoOrden("OP-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .build();
        SolicitudMovimiento pendiente = SolicitudMovimiento.builder()
                .id(222L)
                .estado(EstadoSolicitudMovimiento.ATENDIDA)
                .ordenProduccion(op)
                .detalles(List.of(pendienteDetalle))
                .build();
        SolicitudMovimiento atendida = SolicitudMovimiento.builder()
                .id(223L)
                .estado(EstadoSolicitudMovimiento.CERRADA)
                .ordenProduccion(op)
                .detalles(List.of(atendidoDetalle))
                .build();

        when(repository.findWithDetalles(eq(op.getId()), isNull(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(pendiente, atendida));
        when(almacenRepository.findById(1L)).thenReturn(java.util.Optional.of(origen));
        when(almacenRepository.findById(6L)).thenReturn(java.util.Optional.of(destino));

        SolicitudesPorOrdenDTO dto = service.obtenerPorOrden(op.getId());

        assertThat(dto.getItems()).hasSize(2);
        SolicitudMovimientoItemDTO itemPendiente = dto.getItems().stream()
                .filter(item -> item.getSolicitudId().equals(222L))
                .findFirst()
                .orElseThrow();

        assertThat(itemPendiente.getEstado()).isEqualTo("PENDIENTE");
        assertThat(itemPendiente.getEstadoDetalle()).isEqualTo("PENDIENTE");
        assertThat(itemPendiente.getAlmacenOrigenId()).isEqualTo(1L);
        assertThat(itemPendiente.getAlmacenDestinoId()).isEqualTo(6L);
        assertThat(itemPendiente.getNombreAlmacenOrigen()).isEqualTo("Alm Origen");
        assertThat(itemPendiente.getNombreAlmacenDestino()).isEqualTo("Pre-Bodega Producción");
    }
}
