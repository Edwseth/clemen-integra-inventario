package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudMovimientoServiceImplTest {

    @Mock private SolicitudMovimientoRepository repository;
    @Mock private ProductoRepository productoRepository;
    @Mock private LoteProductoRepository loteRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private OrdenProduccionRepository ordenProduccionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;

    private SolicitudMovimientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudMovimientoServiceImpl(
                repository, productoRepository, loteRepository, almacenRepository,
                ordenProduccionRepository, usuarioRepository, motivoMovimientoRepository,
                tipoMovimientoDetalleRepository, movimientoInventarioRepository
        );
    }

    @Test
    void obtenerSolicitudIncluyeDetalles() {
        Almacen origen = Almacen.builder().id(1).nombre("Orig").build();
        Almacen destino = Almacen.builder().id(2).nombre("Dest").build();
        LoteProducto lote1 = LoteProducto.builder().id(10L).codigoLote("L1").build();
        LoteProducto lote2 = LoteProducto.builder().id(20L).codigoLote("L2").build();
        SolicitudMovimientoDetalle d1 = SolicitudMovimientoDetalle.builder()
                .lote(lote1).cantidad(new BigDecimal("5"))
                .almacenOrigen(origen).almacenDestino(destino).build();
        SolicitudMovimientoDetalle d2 = SolicitudMovimientoDetalle.builder()
                .lote(lote2).cantidad(new BigDecimal("3"))
                .almacenOrigen(origen).almacenDestino(destino).build();
        Producto producto = Producto.builder().id(1).nombre("Prod").codigoSku("SKU").build();
        Usuario solicitante = Usuario.builder().nombreCompleto("User").build();
        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(100L).tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .producto(producto)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .usuarioSolicitante(solicitante)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(List.of(d1, d2))
                .build();
        d1.setSolicitudMovimiento(solicitud);
        d2.setSolicitudMovimiento(solicitud);

        when(repository.findWithDetalles(100L)).thenReturn(Optional.of(solicitud));

        SolicitudMovimientoResponseDTO dto = service.obtenerSolicitud(100L);

        assertEquals(2, dto.getDetalles().size());
        assertEquals(lote1.getId(), dto.getDetalles().get(0).getLoteId());
        assertEquals(lote2.getId(), dto.getDetalles().get(1).getLoteId());
    }

    @Test
    void obtenerPorOrdenAplanaDetalles() {
        Almacen origen = Almacen.builder().id(1).nombre("Orig").ubicacion("U1").build();
        Almacen destino = Almacen.builder().id(2).nombre("Dest").ubicacion("U2").build();
        LoteProducto lote1 = LoteProducto.builder().id(10L).codigoLote("L1").build();
        LoteProducto lote2 = LoteProducto.builder().id(20L).codigoLote("L2").build();
        SolicitudMovimientoDetalle d1 = SolicitudMovimientoDetalle.builder()
                .lote(lote1).cantidad(new BigDecimal("5"))
                .almacenOrigen(origen).almacenDestino(destino).build();
        SolicitudMovimientoDetalle d2 = SolicitudMovimientoDetalle.builder()
                .lote(lote2).cantidad(new BigDecimal("3"))
                .almacenOrigen(origen).almacenDestino(destino).build();
        Producto producto = Producto.builder().id(1).nombre("Prod").build();
        Usuario solicitante = Usuario.builder().nombreCompleto("User").build();
        OrdenProduccion op = OrdenProduccion.builder().id(200L).codigoOrden("OP1").build();

        SolicitudMovimiento s1 = SolicitudMovimiento.builder()
                .id(1L).producto(producto).usuarioSolicitante(solicitante)
                .ordenProduccion(op).estado(EstadoSolicitudMovimiento.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(List.of(d1, d2))
                .build();
        d1.setSolicitudMovimiento(s1);
        d2.setSolicitudMovimiento(s1);

        when(repository.findWithDetalles(200L, null, null, null)).thenReturn(List.of(s1));

        var dto = service.obtenerPorOrden(200L);

        assertEquals(2, dto.getItems().size());
        assertEquals(2, dto.getItemsCount());
        assertEquals(lote1.getId(), dto.getItems().get(0).getLoteId());
        assertEquals(lote2.getId(), dto.getItems().get(1).getLoteId());
    }
}
