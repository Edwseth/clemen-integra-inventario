package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudesPorOrdenDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudMovimientoServiceFiltroOpTest {

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
    @DisplayName("listarPorOrden excluye autorizadas con OP cerrada")
    void listarPorOrden_excluyeAutorizadasConOpCerrada() {
        SolicitudMovimiento autorizadaAbierta = crearSolicitud(1L, "OP-OPEN", EstadoProduccion.EN_PROCESO,
                EstadoSolicitudMovimiento.AUTORIZADA);
        SolicitudMovimiento autorizadaCerrada = crearSolicitud(2L, "OP-CLOSED", EstadoProduccion.FINALIZADA,
                EstadoSolicitudMovimiento.AUTORIZADA);

        when(repository.findWithDetalles(eq(null), eq(List.of(EstadoSolicitudMovimiento.AUTORIZADA)), any(), any(), eq(true), anyList()))
                .thenReturn(List.of(autorizadaAbierta, autorizadaCerrada));

        Page<SolicitudesPorOrdenDTO> pagina = service.listGroupByOrden(
                List.of(EstadoSolicitudMovimiento.AUTORIZADA),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(pagina.getContent())
                .extracting(SolicitudesPorOrdenDTO::getCodigoOrden)
                .containsExactly("OP-OPEN");
    }

    @Test
    @DisplayName("listarPorOrden mantiene pendientes aunque la OP esté cerrada")
    void listarPorOrden_pendientesSinFiltrarPorEstadoOp() {
        SolicitudMovimiento pendienteCerrada = crearSolicitud(3L, "OP-PEND", EstadoProduccion.CERRADA_INCOMPLETA,
                EstadoSolicitudMovimiento.PENDIENTE);

        when(repository.findWithDetalles(eq(null), eq(List.of(EstadoSolicitudMovimiento.PENDIENTE)), any(), any(), eq(false), anyList()))
                .thenReturn(List.of(pendienteCerrada));

        Page<SolicitudesPorOrdenDTO> pagina = service.listGroupByOrden(
                List.of(EstadoSolicitudMovimiento.PENDIENTE),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getCodigoOrden()).isEqualTo("OP-PEND");
    }

    @Test
    @DisplayName("listarPorOrden permite pendientes y autorizadas abiertas en la misma respuesta")
    void listarPorOrden_mixtoRespetaFiltroDeAutorizadas() {
        SolicitudMovimiento pendienteCerrada = crearSolicitud(4L, "OP-MIX-CLOSED", EstadoProduccion.CERRADA_INCOMPLETA,
                EstadoSolicitudMovimiento.PENDIENTE);
        SolicitudMovimiento autorizadaAbierta = crearSolicitud(5L, "OP-MIX-OPEN", EstadoProduccion.EN_PROCESO,
                EstadoSolicitudMovimiento.AUTORIZADA);
        SolicitudMovimiento autorizadaCerrada = crearSolicitud(6L, "OP-MIX-CLOSED", EstadoProduccion.FINALIZADA,
                EstadoSolicitudMovimiento.AUTORIZADA);

        when(repository.findWithDetalles(eq(null), eq(List.of(EstadoSolicitudMovimiento.PENDIENTE, EstadoSolicitudMovimiento.AUTORIZADA)), any(), any(), eq(true), anyList()))
                .thenReturn(List.of(pendienteCerrada, autorizadaAbierta, autorizadaCerrada));

        Page<SolicitudesPorOrdenDTO> pagina = service.listGroupByOrden(
                List.of(EstadoSolicitudMovimiento.PENDIENTE, EstadoSolicitudMovimiento.AUTORIZADA),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(pagina.getContent())
                .extracting(SolicitudesPorOrdenDTO::getCodigoOrden)
                .containsExactly("OP-MIX-OPEN");
    }

    private SolicitudMovimiento crearSolicitud(Long ordenId, String codigo, EstadoProduccion estadoOp,
                                               EstadoSolicitudMovimiento estadoSolicitud) {
        OrdenProduccion op = OrdenProduccion.builder()
                .id(ordenId)
                .codigoOrden(codigo)
                .estado(estadoOp)
                .fechaInicio(LocalDateTime.now())
                .build();

        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Producto prueba");

        Almacen almacen = new Almacen();
        almacen.setId(1);
        almacen.setNombre("ALM-1");

        Almacen almacenDestino = new Almacen();
        almacenDestino.setId(6);
        almacenDestino.setNombre("ALM-DEST");

        LoteProducto lote = new LoteProducto();
        lote.setId(100L);
        lote.setCodigoLote("L-100");
        lote.setProducto(producto);

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(200L)
                .lote(lote)
                .cantidad(BigDecimal.ONE)
                .almacenOrigen(almacen)
                .almacenDestino(almacenDestino)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(ordenId)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .almacenOrigen(almacen)
                .ordenProduccion(op)
                .usuarioSolicitante(new Usuario())
                .estado(estadoSolicitud)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(List.of(detalle))
                .build();
        detalle.setSolicitudMovimiento(solicitud);
        return solicitud;
    }
}
