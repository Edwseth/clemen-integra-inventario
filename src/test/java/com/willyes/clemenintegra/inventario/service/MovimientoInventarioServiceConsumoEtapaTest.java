package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceConsumoEtapaTest {

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
    private MovimientoInventarioMapper mapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private ReservaLoteService reservaLoteService;
    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private RecepcionOCService recepcionOCService;
    @Mock
    private LoteCalidadValidator loteCalidadValidator;
    @Mock
    private EntityManager entityManager;
    @Mock
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;

    @Spy
    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @Test
    void consumirInsumosPorOrden_asociaEtapaEnSalidaProduccion() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        SolicitudMovimientoDetalle detalle = solicitud.getDetalles().get(0);
        Producto producto = solicitud.getProducto();

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(70L);

        LoteProducto lotePrebodega = new LoteProducto();
        lotePrebodega.setId(300L);
        lotePrebodega.setCodigoLote(detalle.getLote().getCodigoLote());
        lotePrebodega.setProducto(producto);
        lotePrebodega.setAlmacen(new Almacen(30));
        lotePrebodega.setEstado(EstadoLote.DISPONIBLE);
        lotePrebodega.setStockLote(new BigDecimal("50"));
        lotePrebodega.setStockReservado(BigDecimal.ZERO);

        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                detalle.getLote().getCodigoLote(), producto.getId(), 30))
                .thenReturn(Optional.of(lotePrebodega));
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(
                eq(solicitud.getId()),
                eq(producto.getId().longValue()),
                eq(lotePrebodega.getId()),
                eq(TipoMovimiento.SALIDA),
                eq(70L),
                isNull())).thenReturn(BigDecimal.ZERO);

        doReturn(MovimientoInventarioResponseDTO.builder().id(999L).build())
                .when(service).registrarMovimiento(any(MovimientoInventarioDTO.class));

        service.consumirInsumosPorOrden(10L, 20L, 5L);

        ArgumentCaptor<MovimientoInventarioDTO> dtoCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(service).registrarMovimiento(dtoCaptor.capture());

        MovimientoInventarioDTO dtoSalida = dtoCaptor.getValue();
        assertThat(dtoSalida.ordenProduccionId()).isEqualTo(10L);
        assertThat(dtoSalida.ordenProduccionEtapaId()).isEqualTo(20L);
        assertThat(dtoSalida.solicitudMovimientoId()).isEqualTo(solicitud.getId());
        assertThat(detalle.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
    }

    @Test
    void consumirInsumosPorOrden_sinLotePrebodegaLanzaError() {
        SolicitudMovimiento solicitud = solicitudConDetalle();

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(70L);
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumirInsumosPorOrden(10L, 20L, 5L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.CONSUMO_PREBODEGA_INSUFICIENTE);
    }

    @Test
    void consumirInsumosPorOrden_resuelveEtapaCuandoNoSeEnvio() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        SolicitudMovimientoDetalle detalle = solicitud.getDetalles().get(0);
        Producto producto = solicitud.getProducto();

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(70L);
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(10L))
                .thenReturn(List.of(EtapaProduccion.builder().id(22L).build()));

        LoteProducto lotePrebodega = new LoteProducto();
        lotePrebodega.setId(300L);
        lotePrebodega.setCodigoLote(detalle.getLote().getCodigoLote());
        lotePrebodega.setProducto(producto);
        lotePrebodega.setAlmacen(new Almacen(30));
        lotePrebodega.setEstado(EstadoLote.DISPONIBLE);
        lotePrebodega.setStockLote(new BigDecimal("50"));
        lotePrebodega.setStockReservado(BigDecimal.ZERO);

        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                detalle.getLote().getCodigoLote(), producto.getId(), 30))
                .thenReturn(Optional.of(lotePrebodega));
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(
                eq(solicitud.getId()),
                eq(producto.getId().longValue()),
                eq(lotePrebodega.getId()),
                eq(TipoMovimiento.SALIDA),
                eq(70L),
                isNull())).thenReturn(BigDecimal.ZERO);

        doReturn(MovimientoInventarioResponseDTO.builder().id(999L).build())
                .when(service).registrarMovimiento(any(MovimientoInventarioDTO.class));

        service.consumirInsumosPorOrden(10L, null, 5L);

        ArgumentCaptor<MovimientoInventarioDTO> dtoCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(service).registrarMovimiento(dtoCaptor.capture());

        MovimientoInventarioDTO dtoSalida = dtoCaptor.getValue();
        assertThat(dtoSalida.ordenProduccionEtapaId()).isEqualTo(22L);
    }

    private SolicitudMovimiento solicitudConDetalle() {
        Producto producto = new Producto();
        producto.setId(1);

        LoteProducto lote = new LoteProducto();
        lote.setId(100L);
        lote.setCodigoLote("LOT-001");
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(10));
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setStockLote(new BigDecimal("20"));
        lote.setStockReservado(BigDecimal.ZERO);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(200L);
        detalle.setCantidad(new BigDecimal("10"));
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setLote(lote);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(10L);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(500L);
        solicitud.setProducto(producto);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setOrdenProduccion(ordenProduccion);
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        return solicitud;
    }
}
