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
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(20L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .ordenProduccion(OrdenProduccion.builder().id(10L).build())
                .build();
        when(etapaProduccionRepository.findById(anyLong())).thenReturn(Optional.of(etapaActiva));

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
    void consumirInsumosPorOrden_conMultiplesLotesPropagaEtapaEnCadaMovimiento() {
        SolicitudMovimiento solicitud = solicitudConDosDetalles();
        SolicitudMovimientoDetalle detalle1 = solicitud.getDetalles().get(0);
        SolicitudMovimientoDetalle detalle2 = solicitud.getDetalles().get(1);
        Producto producto = solicitud.getProducto();

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(70L);
        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(20L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .ordenProduccion(OrdenProduccion.builder().id(10L).build())
                .build();
        when(etapaProduccionRepository.findById(anyLong())).thenReturn(Optional.of(etapaActiva));

        LoteProducto lotePrebodega1 = loteEnPrebodega(detalle1.getLote(), producto, 30);
        LoteProducto lotePrebodega2 = loteEnPrebodega(detalle2.getLote(), producto, 30);

        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                detalle1.getLote().getCodigoLote(), producto.getId(), 30))
                .thenReturn(Optional.of(lotePrebodega1));
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                detalle2.getLote().getCodigoLote(), producto.getId(), 30))
                .thenReturn(Optional.of(lotePrebodega2));

        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(
                eq(solicitud.getId()),
                eq(producto.getId().longValue()),
                anyLong(),
                eq(TipoMovimiento.SALIDA),
                eq(70L),
                isNull())).thenReturn(BigDecimal.ZERO);

        doReturn(MovimientoInventarioResponseDTO.builder().id(999L).build())
                .when(service).registrarMovimiento(any(MovimientoInventarioDTO.class));

        service.consumirInsumosPorOrden(10L, 20L, 5L);

        ArgumentCaptor<MovimientoInventarioDTO> dtoCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(service, times(2)).registrarMovimiento(dtoCaptor.capture());

        assertThat(dtoCaptor.getAllValues())
                .extracting(MovimientoInventarioDTO::ordenProduccionEtapaId)
                .containsOnly(20L);
        assertThat(detalle1.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        assertThat(detalle2.getEstado()).isEqualTo(EstadoSolicitudMovimientoDetalle.ATENDIDO);
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
        EtapaProduccion etapaActivaError = EtapaProduccion.builder()
                .id(20L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .ordenProduccion(OrdenProduccion.builder().id(10L).build())
                .build();
        when(etapaProduccionRepository.findById(anyLong())).thenReturn(Optional.of(etapaActivaError));
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
        EtapaProduccion activa = EtapaProduccion.builder()
                        .id(22L)
                        .estado(EstadoEtapa.EN_PROCESO)
                        .fechaInicio(LocalDateTime.now())
                        .build();
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(10L))
                .thenReturn(1L);
        when(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(10L))
                .thenReturn(Optional.of(activa));

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

    @Test
    void consumirInsumosPorOrden_utilizaEtapaActiva() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        SolicitudMovimientoDetalle detalle = solicitud.getDetalles().get(0);
        Producto producto = solicitud.getProducto();

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(70L);
        EtapaProduccion activa = EtapaProduccion.builder()
                .id(30L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .build();
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(10L))
                .thenReturn(1L);
        when(etapaProduccionRepository.findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(10L))
                .thenReturn(Optional.of(activa));

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
        assertThat(dtoSalida.ordenProduccionEtapaId()).isEqualTo(30L);
    }

    @Test
    void consumirInsumosPorOrden_sinEtapaActivaLanzaError() {
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(10L))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.consumirInsumosPorOrden(10L, null, 5L))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.OP_SIN_ETAPA_ACTIVA);
    }

    @Test
    void resolverEtapaActiva_respetaEtapaEnProcesoUnica() {
        EtapaProduccion activa = EtapaProduccion.builder()
                .id(1L)
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(5L).build())
                .build();
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(5L))
                .thenReturn(1L);
        when(etapaProduccionRepository
                .findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(5L))
                .thenReturn(Optional.of(activa));

        EtapaProduccion resultado = ReflectionTestUtils.invokeMethod(service, "resolverEtapaActiva", 5L, null);

        assertThat(resultado).isSameAs(activa);
    }

    @Test
    void resolverEtapaActiva_sinActivasLanzaError() {
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(9L))
                .thenReturn(0L);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "resolverEtapaActiva", 9L, null))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.OP_SIN_ETAPA_ACTIVA);
    }

    @Test
    void resolverEtapaActiva_conMultiplesActivasLanzaConflicto() {
        EtapaProduccion e1 = EtapaProduccion.builder().id(1L).estado(EstadoEtapa.EN_PROCESO).build();
        EtapaProduccion e2 = EtapaProduccion.builder().id(2L).estado(EstadoEtapa.EN_PROCESO).build();
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(15L))
                .thenReturn(2L);
        when(etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(15L))
                .thenReturn(List.of(e1, e2));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "resolverEtapaActiva", 15L, null))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.PRODUCCION_MULTIPLES_ETAPAS_ACTIVAS);
    }

    @Test
    void resolverEtapaActiva_conIdNoConsultaActivas() {
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(11L)
                .estado(EstadoEtapa.EN_PROCESO)
                .ordenProduccion(OrdenProduccion.builder().id(7L).build())
                .build();
        when(etapaProduccionRepository.findById(11L)).thenReturn(Optional.of(etapa));

        EtapaProduccion resultado = ReflectionTestUtils.invokeMethod(service, "resolverEtapaActiva", 7L, 11L);

        assertThat(resultado).isSameAs(etapa);
        verify(etapaProduccionRepository, never()).countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(anyLong());
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

    private SolicitudMovimiento solicitudConDosDetalles() {
        Producto producto = new Producto();
        producto.setId(1);

        LoteProducto lote1 = new LoteProducto();
        lote1.setId(100L);
        lote1.setCodigoLote("LOT-001");
        lote1.setProducto(producto);
        lote1.setAlmacen(new Almacen(10));
        lote1.setEstado(EstadoLote.DISPONIBLE);
        lote1.setStockLote(new BigDecimal("10"));
        lote1.setStockReservado(BigDecimal.ZERO);

        LoteProducto lote2 = new LoteProducto();
        lote2.setId(101L);
        lote2.setCodigoLote("LOT-002");
        lote2.setProducto(producto);
        lote2.setAlmacen(new Almacen(10));
        lote2.setEstado(EstadoLote.DISPONIBLE);
        lote2.setStockLote(new BigDecimal("15"));
        lote2.setStockReservado(BigDecimal.ZERO);

        SolicitudMovimientoDetalle detalle1 = new SolicitudMovimientoDetalle();
        detalle1.setId(200L);
        detalle1.setCantidad(new BigDecimal("5"));
        detalle1.setCantidadAtendida(BigDecimal.ZERO);
        detalle1.setLote(lote1);

        SolicitudMovimientoDetalle detalle2 = new SolicitudMovimientoDetalle();
        detalle2.setId(201L);
        detalle2.setCantidad(new BigDecimal("7"));
        detalle2.setCantidadAtendida(BigDecimal.ZERO);
        detalle2.setLote(lote2);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(10L);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(500L);
        solicitud.setProducto(producto);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setOrdenProduccion(ordenProduccion);
        solicitud.setDetalles(List.of(detalle1, detalle2));
        detalle1.setSolicitudMovimiento(solicitud);
        detalle2.setSolicitudMovimiento(solicitud);

        return solicitud;
    }

    private LoteProducto loteEnPrebodega(LoteProducto base, Producto producto, int almacenId) {
        LoteProducto lote = new LoteProducto();
        lote.setId(base.getId() + 1000);
        lote.setCodigoLote(base.getCodigoLote());
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setStockLote(base.getStockLote());
        lote.setStockReservado(BigDecimal.ZERO);
        return lote;
    }
}
