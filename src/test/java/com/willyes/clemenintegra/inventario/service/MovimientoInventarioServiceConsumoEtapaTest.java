package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
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
import static org.mockito.Mockito.lenient;
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
    private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
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
    @Mock
    private CosteoInventarioService costeoInventarioService;

    @Spy
    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void defaultCosteoInventario() {
        lenient().when(costeoInventarioService.calcularCostoUnitarioRecepcion(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoTotalLineaRecepcion(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoUnitarioPromedioPorIngreso(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
        lenient().when(costeoInventarioService.calcularCostoTotalMovimiento(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));
    }

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
        assertThat(dtoSalida.loteProductoId()).isEqualTo(lotePrebodega.getId());
        assertThat(dtoSalida.atenciones()).hasSize(1);
        assertThat(dtoSalida.atenciones().get(0).getDetalleId()).isEqualTo(detalle.getId());
        assertThat(dtoSalida.atenciones().get(0).getLoteId()).isEqualTo(detalle.getLote().getId());
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
    void procesarMovimientoConLoteExistente_paraSalidaProduccionOpRetornaLoteFisicoPrebodega() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        SolicitudMovimientoDetalle detalle = solicitud.getDetalles().get(0);
        Producto producto = solicitud.getProducto();

        LoteProducto loteReservado = detalle.getLote();
        loteReservado.setStockReservado(new BigDecimal("10"));

        LoteProducto lotePrebodega = loteEnPrebodega(loteReservado, producto, 30);
        lotePrebodega.setStockLote(new BigDecimal("10"));
        lotePrebodega.setStockReservado(BigDecimal.ZERO);

        when(loteProductoRepository.findByIdForUpdate(loteReservado.getId())).thenReturn(Optional.of(loteReservado));
        when(loteProductoRepository.findByIdForUpdate(lotePrebodega.getId())).thenReturn(Optional.of(lotePrebodega));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudMovimientoDetalleRepository.save(any(SolicitudMovimientoDetalle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(solicitudMovimientoRepository.saveAndFlush(any(SolicitudMovimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                null,
                null,
                null,
                producto.getId(),
                lotePrebodega.getId(),
                30,
                null,
                null,
                null,
                null,
                null,
                solicitud.getId(),
                null,
                solicitud.getOrdenProduccion().getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(crearAtencion(detalle.getId(), loteReservado.getId(), new BigDecimal("10"))),
                null,
                null
        );

        @SuppressWarnings("unchecked")
        List<Object> resultado = (List<Object>) ReflectionTestUtils.invokeMethod(
                service,
                "procesarMovimientoConLoteExistente",
                dto,
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                new Almacen(30),
                null,
                producto,
                new BigDecimal("10"),
                false,
                solicitud,
                null
        );

        assertThat(resultado).hasSize(1);
        Object detalleResultado = resultado.get(0);
        LoteProducto loteResultado = (LoteProducto) ReflectionTestUtils.getField(detalleResultado, "lote");
        BigDecimal cantidadResultado = (BigDecimal) ReflectionTestUtils.getField(detalleResultado, "cantidad");

        assertThat(loteResultado.getId()).isEqualTo(lotePrebodega.getId());
        assertThat(cantidadResultado).isEqualByComparingTo("10");
        assertThat(lotePrebodega.getStockLote()).isEqualByComparingTo("0");
        assertThat(lotePrebodega.isAgotado()).isTrue();
        verify(reservaLoteService).consumirReserva(solicitud, detalle, loteReservado, new BigDecimal("10.0000"));
    }

    @Test
    void consumirInsumosPorOrden_sinLotePrebodegaLanzaError() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        SolicitudMovimientoDetalle detalle = solicitud.getDetalles().get(0);
        Producto producto = solicitud.getProducto();

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
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                detalle.getLote().getCodigoLote(), producto.getId(), 30))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumirInsumosPorOrden(10L, 20L, 5L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.CONSUMO_PREBODEGA_INSUFICIENTE);
    }

    @Test
    void consumirInsumosPorOrden_sinControlStockOmiteConsumo() {
        SolicitudMovimiento solicitud = solicitudConDetalle();
        solicitud.getProducto().setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);

        TypedQuery<SolicitudMovimiento> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(SolicitudMovimiento.class))).thenReturn(query);
        when(query.setParameter(eq("opId"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(solicitud));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(30L);
        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(20L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now())
                .ordenProduccion(OrdenProduccion.builder().id(10L).build())
                .build();
        when(etapaProduccionRepository.findById(anyLong())).thenReturn(Optional.of(etapaActiva));

        service.consumirInsumosPorOrden(10L, 20L, 5L);

        verify(service, never()).registrarMovimiento(any(MovimientoInventarioDTO.class));
        verify(loteProductoRepository, never()).findByCodigoLoteAndProductoIdAndAlmacenId(anyString(), anyInt(), anyInt());
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
                .isEqualTo(ApiErrorCode.OP_MULTIPLES_ETAPAS_ACTIVAS);
    }

    @Test
    void requiereEtapaActiva_paraPrearranqueNoExigeEtapa() {
        boolean requiere = ReflectionTestUtils.invokeMethod(
                service,
                "requiereEtapaActiva",
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                100L,
                null,
                null
        );

        assertThat(requiere).isFalse();
    }

    @Test
    void requiereEtapaActiva_paraSalidaProduccionExigeEtapa() {
        boolean requiere = ReflectionTestUtils.invokeMethod(
                service,
                "requiereEtapaActiva",
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                200L,
                null,
                10L
        );

        assertThat(requiere).isTrue();
    }

    @Test
    void requiereEtapaActiva_cuandoDtoIncluyeEtapaExigeValidacion() {
        boolean requiere = ReflectionTestUtils.invokeMethod(
                service,
                "requiereEtapaActiva",
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                200L,
                55L,
                10L
        );

        assertThat(requiere).isTrue();
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

    @Test
    void resolverEtapaActiva_conEtapaFinalizadaPermiteConsumo() {
        LocalDateTime fin = LocalDateTime.now();
        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(12L)
                .estado(EstadoEtapa.FINALIZADA)
                .fechaInicio(fin.minusHours(2))
                .fechaFin(fin.minusHours(1))
                .ordenProduccion(OrdenProduccion.builder().id(8L).build())
                .build();
        when(etapaProduccionRepository.findById(12L)).thenReturn(Optional.of(etapa));

        EtapaProduccion resultado = ReflectionTestUtils.invokeMethod(service, "resolverEtapaActiva", 8L, 12L);

        assertThat(resultado).isSameAs(etapa);
    }

    private SolicitudMovimiento solicitudConDetalle() {
        Producto producto = new Producto();
        producto.setId(1);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(10L);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(500L);
        solicitud.setProducto(producto);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setOrdenProduccion(ordenProduccion);
        SolicitudMovimientoDetalle detalle = buildDetalleCompleto(
                200L,
                producto,
                100L,
                "LOT-001",
                10L,
                20L,
                new BigDecimal("10")
        );
        solicitud.setDetalles(List.of(detalle));
        detalle.setSolicitudMovimiento(solicitud);

        return solicitud;
    }

    private SolicitudMovimiento solicitudConDosDetalles() {
        Producto producto = new Producto();
        producto.setId(1);

        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(10L);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(500L);
        solicitud.setProducto(producto);
        solicitud.setTipoMovimiento(TipoMovimiento.SALIDA);
        solicitud.setOrdenProduccion(ordenProduccion);
        SolicitudMovimientoDetalle detalle1 = buildDetalleCompleto(
                200L,
                producto,
                100L,
                "LOT-001",
                10L,
                20L,
                new BigDecimal("5")
        );
        SolicitudMovimientoDetalle detalle2 = buildDetalleCompleto(
                201L,
                producto,
                101L,
                "LOT-002",
                10L,
                20L,
                new BigDecimal("7")
        );
        solicitud.setDetalles(List.of(detalle1, detalle2));
        detalle1.setSolicitudMovimiento(solicitud);
        detalle2.setSolicitudMovimiento(solicitud);

        return solicitud;
    }

    private SolicitudMovimientoDetalle buildDetalleCompleto(
            Long detalleId,
            Producto producto,
            Long loteId,
            String codigoLote,
            Long almacenOrigenId,
            Long almacenDestinoId,
            BigDecimal cantidad
    ) {
        LoteProducto lote = new LoteProducto();
        lote.setId(loteId);
        lote.setCodigoLote(codigoLote);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenOrigenId.intValue()));
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setStockLote(cantidad);
        lote.setStockReservado(BigDecimal.ZERO);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setId(detalleId);
        detalle.setEstado(EstadoSolicitudMovimientoDetalle.PENDIENTE);
        detalle.setCantidad(cantidad);
        detalle.setCantidadAtendida(BigDecimal.ZERO);
        detalle.setLote(lote);
        detalle.setAlmacenOrigen(new Almacen(almacenOrigenId.intValue()));
        detalle.setAlmacenDestino(new Almacen(almacenDestinoId.intValue()));

        return detalle;
    }


    private AtencionDTO crearAtencion(Long detalleId, Long loteId, BigDecimal cantidad) {
        AtencionDTO atencion = new AtencionDTO();
        atencion.setDetalleId(detalleId);
        atencion.setLoteId(loteId);
        atencion.setCantidad(cantidad);
        return atencion;
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
