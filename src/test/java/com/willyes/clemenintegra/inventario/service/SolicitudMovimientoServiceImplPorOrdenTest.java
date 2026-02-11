package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoItemDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
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
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void obtenerPorOrdenSerializaDatosDesdeDetalle() {
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
                .almacenOrigen(origen)
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
                .almacenOrigen(null)
                .almacenDestino(null)
                .lote(null)
                .detalles(List.of(pendienteDetalle))
                .build();
        SolicitudMovimiento atendida = SolicitudMovimiento.builder()
                .id(223L)
                .estado(EstadoSolicitudMovimiento.CERRADA)
                .ordenProduccion(op)
                .almacenOrigen(null)
                .almacenDestino(null)
                .lote(null)
                .detalles(List.of(atendidoDetalle))
                .build();

        when(repository.findWithDetalles(eq(op.getId()), isNull(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(pendiente, atendida));
        SolicitudesPorOrdenDTO dto = service.obtenerPorOrden(op.getId());

        assertThat(dto.getItems()).hasSize(2);
        SolicitudMovimientoItemDTO itemPendiente = dto.getItems().stream()
                .filter(item -> item.getSolicitudId().equals(222L))
                .findFirst()
                .orElseThrow();

        assertThat(itemPendiente.getDetalleId()).isEqualTo(228L);
        assertThat(itemPendiente.getEstado()).isEqualTo("PENDIENTE");
        assertThat(itemPendiente.getEstadoDetalle()).isEqualTo("PENDIENTE");
        assertThat(itemPendiente.getAlmacenOrigenId()).isEqualTo(1L);
        assertThat(itemPendiente.getAlmacenDestinoId()).isEqualTo(6L);
        assertThat(itemPendiente.getNombreAlmacenOrigen()).isEqualTo("Alm Origen");
        assertThat(itemPendiente.getNombreAlmacenDestino()).isEqualTo("Pre-Bodega Producción");
    }

    @Test
    void listGroupByOrdenSerializaDetalleYEstadoSinContaminacion() {
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
                .id(2148L)
                .codigoLote("L-2148")
                .almacen(origen)
                .build();
        SolicitudMovimientoDetalle pendienteDetalle = SolicitudMovimientoDetalle.builder()
                .id(228L)
                .lote(lote)
                .cantidad(BigDecimal.TEN)
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
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
                .id(38L)
                .codigoOrden("OP-038")
                .fechaInicio(LocalDateTime.now())
                .build();
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(222L)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .ordenProduccion(op)
                .almacenOrigen(null)
                .almacenDestino(null)
                .lote(null)
                .detalles(List.of(pendienteDetalle, atendidoDetalle))
                .build();

        when(repository.findWithDetalles(isNull(), anyList(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(solicitud));

        SolicitudesPorOrdenDTO dto = service.listGroupByOrden(
                List.of(EstadoSolicitudMovimiento.PENDIENTE), null, null, Pageable.ofSize(10))
                .getContent().get(0);

        assertThat(dto.getEstadoAgregado()).isEqualTo("MIXTO");
        SolicitudMovimientoItemDTO itemPendiente = dto.getItems().stream()
                .filter(item -> item.getDetalleId().equals(228L))
                .findFirst()
                .orElseThrow();

        assertThat(itemPendiente.getLoteId()).isEqualTo(2148L);
        assertThat(itemPendiente.getAlmacenOrigenId()).isEqualTo(1L);
        assertThat(itemPendiente.getAlmacenDestinoId()).isEqualTo(6L);
        assertThat(itemPendiente.getEstadoDetalle()).isEqualTo("PENDIENTE");
        assertThat(itemPendiente.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void listGroupByOrdenRechazaDetalleIncompleto() {
        SolicitudMovimientoDetalle pendienteDetalle = SolicitudMovimientoDetalle.builder()
                .id(228L)
                .cantidad(BigDecimal.TEN)
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(null)
                .almacenOrigen(null)
                .almacenDestino(null)
                .lote(null)
                .build();
        OrdenProduccion op = OrdenProduccion.builder()
                .id(100L)
                .codigoOrden("OP-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .build();
        SolicitudMovimiento pendiente = SolicitudMovimiento.builder()
                .id(222L)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .ordenProduccion(op)
                .detalles(List.of(pendienteDetalle))
                .build();

        when(repository.findWithDetalles(isNull(), anyList(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(pendiente));

        assertThatThrownBy(() -> service.listGroupByOrden(
                List.of(EstadoSolicitudMovimiento.PENDIENTE), null, null, Pageable.ofSize(10)))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException cbe = (CustomBusinessException) ex;
                    assertThat(cbe.getCode()).isEqualTo(ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO);
                });
    }

    @Test
    void obtenerPorOrdenRechazaDetalleIncompleto() {
        SolicitudMovimientoDetalle pendienteDetalle = SolicitudMovimientoDetalle.builder()
                .id(228L)
                .cantidad(BigDecimal.TEN)
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(null)
                .almacenOrigen(null)
                .almacenDestino(null)
                .lote(null)
                .build();
        OrdenProduccion op = OrdenProduccion.builder()
                .id(100L)
                .codigoOrden("OP-TEST-001")
                .fechaInicio(LocalDateTime.now())
                .build();
        SolicitudMovimiento pendiente = SolicitudMovimiento.builder()
                .id(222L)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .ordenProduccion(op)
                .detalles(List.of(pendienteDetalle))
                .build();

        when(repository.findWithDetalles(eq(op.getId()), isNull(), isNull(), isNull(), eq(false), anyList()))
                .thenReturn(List.of(pendiente));

        assertThatThrownBy(() -> service.obtenerPorOrden(op.getId()))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException cbe = (CustomBusinessException) ex;
                    assertThat(cbe.getCode()).isEqualTo(ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO);
                });
    }

    @Test
    void obtenerSolicitudIncluyeIdsDetalleYLoteProducto() {
        LoteProducto lote = LoteProducto.builder()
                .id(60L)
                .codigoLote("L-060")
                .build();
        SolicitudMovimientoDetalle detalle1 = SolicitudMovimientoDetalle.builder()
                .id(501L)
                .lote(lote)
                .cantidad(BigDecimal.ONE)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .build();
        SolicitudMovimientoDetalle detalle2 = SolicitudMovimientoDetalle.builder()
                .id(502L)
                .lote(lote)
                .cantidad(BigDecimal.TEN)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .build();
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(114L)
                .detalles(List.of(detalle1, detalle2))
                .build();

        when(repository.findById(eq(114L))).thenReturn(java.util.Optional.of(solicitud));

        SolicitudMovimientoResponseDTO respuesta = service.obtenerSolicitud(114L);

        assertThat(respuesta.getDetalles()).hasSize(2);
        assertThat(respuesta.getDetalles().get(0).getId()).isEqualTo(501L);
        assertThat(respuesta.getDetalles().get(0).getLoteProductoId()).isEqualTo(60L);
        assertThat(respuesta.getDetalles().get(0).getLoteId()).isEqualTo(60L);
        assertThat(respuesta.getDetalles().get(1).getId()).isEqualTo(502L);
        assertThat(respuesta.getDetalles().get(1).getLoteProductoId()).isEqualTo(60L);
        assertThat(respuesta.getDetalles().get(1).getLoteId()).isEqualTo(60L);
    }
    @Test
    void obtenerSolicitudNoDependeDeFindWithDetalles() {
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(999L)
                .detalles(List.of(SolicitudMovimientoDetalle.builder()
                        .id(701L)
                        .cantidad(BigDecimal.ONE)
                        .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                        .build()))
                .build();

        when(repository.findById(eq(999L))).thenReturn(java.util.Optional.of(solicitud));

        SolicitudMovimientoResponseDTO respuesta = service.obtenerSolicitud(999L);

        assertThat(respuesta.getId()).isEqualTo(999L);
        assertThat(respuesta.getDetalles()).hasSize(1);
        verify(repository, never()).findWithDetalles(999L);
    }

}
