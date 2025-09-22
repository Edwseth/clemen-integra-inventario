package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceImplTest {

    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private OrdenCompraService ordenCompraService;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver inventoryCatalogResolver;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @Test
    @SuppressWarnings("unchecked")
    void permiteTransferenciaUsandoReservaPendienteCuandoSolicitudAutorizada() {
        Almacen origen = new Almacen();
        origen.setId(1);
        origen.setNombre("Origen");
        origen.setUbicacion("Ubicacion");
        origen.setCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        origen.setTipo(TipoAlmacen.PRINCIPAL);

        Almacen destino = new Almacen();
        destino.setId(2);
        destino.setNombre("Destino");
        destino.setUbicacion("Ubicacion");
        destino.setCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        destino.setTipo(TipoAlmacen.PRINCIPAL);

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(10L);
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(100);
        producto.setCategoriaProducto(categoria);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto loteOrigen = new LoteProducto();
        loteOrigen.setId(50L);
        loteOrigen.setCodigoLote("LOT-001");
        loteOrigen.setAlmacen(origen);
        loteOrigen.setProducto(producto);
        loteOrigen.setEstado(EstadoLote.DISPONIBLE);
        loteOrigen.setStockLote(new BigDecimal("10.00"));
        loteOrigen.setStockReservado(new BigDecimal("10.000000"));

        LoteProducto loteDestino = new LoteProducto();
        loteDestino.setId(51L);
        loteDestino.setCodigoLote("LOT-001");
        loteDestino.setAlmacen(destino);
        loteDestino.setProducto(producto);
        loteDestino.setEstado(EstadoLote.DISPONIBLE);
        loteDestino.setStockLote(new BigDecimal("0.00"));
        loteDestino.setStockReservado(new BigDecimal("0.000000"));

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(200L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .producto(producto)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detalle = SolicitudMovimientoDetalle.builder()
                .id(300L)
                .solicitudMovimiento(solicitud)
                .lote(loteOrigen)
                .cantidad(new BigDecimal("10.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .build();
        solicitud.getDetalles().add(detalle);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10.000000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                producto.getId(),
                loteOrigen.getId(),
                origen.getId(),
                destino.getId(),
                null,
                null,
                null,
                99L,
                solicitud.getId(),
                null,
                null,
                null,
                loteOrigen.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                List.of()
        );

        when(loteProductoRepository.findByIdForUpdate(loteOrigen.getId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(
                producto.getId(), loteOrigen.getCodigoLote(), destino.getId()))
                .thenReturn(Optional.of(loteDestino));
        when(loteProductoRepository.save(any(LoteProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<MovimientoInventarioServiceImpl.MovimientoLoteDetalle> resultado =
                (List<MovimientoInventarioServiceImpl.MovimientoLoteDetalle>) ReflectionTestUtils.invokeMethod(
                        service,
                        "procesarMovimientoConLoteExistente",
                        dto,
                        TipoMovimiento.TRANSFERENCIA,
                        origen,
                        destino,
                        producto,
                        new BigDecimal("10.000000"),
                        false,
                        solicitud
                );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).lote()).isEqualTo(loteDestino);
        assertThat(resultado.get(0).cantidad()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void atiendeSolicitudConMultiplesDetallesDelMismoLoteSinExcederPendiente() {
        Almacen origen = new Almacen();
        origen.setId(3);

        Producto producto = new Producto();
        producto.setId(500);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);

        LoteProducto lote = new LoteProducto();
        lote.setId(600L);
        lote.setProducto(producto);
        lote.setAlmacen(origen);
        lote.setStockLote(new BigDecimal("150.00"));
        lote.setStockReservado(new BigDecimal("100.000000"));

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(700L)
                .estado(EstadoSolicitudMovimiento.AUTORIZADA)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .lote(lote)
                .almacenOrigen(origen)
                .fechaSolicitud(LocalDateTime.now())
                .detalles(new java.util.ArrayList<>())
                .build();

        SolicitudMovimientoDetalle detalle12 = SolicitudMovimientoDetalle.builder()
                .id(710L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("12.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        SolicitudMovimientoDetalle detalle88 = SolicitudMovimientoDetalle.builder()
                .id(711L)
                .solicitudMovimiento(solicitud)
                .lote(lote)
                .cantidad(new BigDecimal("88.000000"))
                .cantidadAtendida(BigDecimal.ZERO)
                .estado(EstadoSolicitudMovimientoDetalle.PENDIENTE)
                .almacenOrigen(origen)
                .build();

        solicitud.getDetalles().add(detalle12);
        solicitud.getDetalles().add(detalle88);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("100.000000"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                producto.getId(),
                lote.getId(),
                origen.getId(),
                null,
                null,
                null,
                null,
                null,
                123L,
                solicitud.getId(),
                null,
                null,
                null,
                null,
                null,
                EstadoLote.DISPONIBLE,
                Boolean.FALSE,
                List.of()
        );

        when(loteProductoRepository.findByIdForUpdate(lote.getId())).thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudMovimientoDetalleRepository.findById(detalle12.getId())).thenReturn(Optional.of(detalle12));
        when(solicitudMovimientoDetalleRepository.findById(detalle88.getId())).thenReturn(Optional.of(detalle88));
        when(solicitudMovimientoDetalleRepository.save(any(SolicitudMovimientoDetalle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservaLoteRepository.sumPendienteActivaByLoteId(eq(lote.getId()), eq(EstadoReservaLote.ACTIVA)))
                .thenReturn(BigDecimal.ZERO);

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                service,
                "atenderSolicitudMovimiento",
                dto,
                solicitud
        )).doesNotThrowAnyException();

        ArgumentCaptor<SolicitudMovimientoDetalle> detalleCaptor = ArgumentCaptor.forClass(SolicitudMovimientoDetalle.class);
        ArgumentCaptor<BigDecimal> cantidadCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(reservaLoteService, times(2)).consumirReserva(
                eq(solicitud),
                detalleCaptor.capture(),
                eq(lote),
                cantidadCaptor.capture()
        );

        assertThat(detalleCaptor.getAllValues()).extracting(SolicitudMovimientoDetalle::getId)
                .containsExactly(detalle12.getId(), detalle88.getId());
        assertThat(cantidadCaptor.getAllValues())
                .containsExactly(new BigDecimal("12.000000"), new BigDecimal("88.000000"));
    }
}
