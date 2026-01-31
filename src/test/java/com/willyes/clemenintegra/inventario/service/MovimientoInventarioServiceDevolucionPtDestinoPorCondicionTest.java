package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.CausaDevolucionPT;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.CondicionProductoDevuelto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceDevolucionPtDestinoPorCondicionTest {

    private static final long ALMACEN_PT_ID = 2L;
    private static final long ALMACEN_CUARENTENA_ID = 7L;
    private static final long TIPO_DETALLE_ENTRADA_ID = 15L;
    private static final long TIPO_DETALLE_TRANSFERENCIA_ID = 16L;
    private static final long MOTIVO_ENTRADA_PT_ID = 21L;
    private static final long MOTIVO_TRANSFERENCIA_CALIDAD_ID = 22L;

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

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setupSecurity() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tester", "secret");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        given(usuarioService.obtenerUsuarioAutenticado()).willReturn(
                Usuario.builder().id(1L).nombreCompleto("Tester").build()
        );
        stubCatalogosRecepcionDevolucion();
        stubPersistenciaLoteDestino();
    }

    @Test
    void shouldRouteToPt_whenCondicionOptimo() {
        Producto producto = crearProducto(100);
        LoteProducto lote = crearLote(400L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.CORTA_FECHA, CondicionProductoDevuelto.OPTIMO);

        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_ENTRADA_ID);
        ArgumentCaptor<LoteProducto> loteCaptor = capturarLoteGuardado(dto);

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_PT_ID);
        assertThat(loteCaptor.getValue().getEstado()).isNotEqualTo(EstadoLote.EN_CUARENTENA);
    }

    @Test
    void shouldRouteToCuarentena_whenCondicionDudoso() {
        Producto producto = crearProducto(200);
        LoteProducto lote = crearLote(500L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.DUDOSO);

        configurarMocksBasicos(producto, lote, ALMACEN_CUARENTENA_ID, TIPO_DETALLE_TRANSFERENCIA_ID);
        ArgumentCaptor<LoteProducto> loteCaptor = capturarLoteGuardado(dto);

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_CUARENTENA_ID);
        assertThat(loteCaptor.getValue().getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
    }

    @Test
    void shouldPersistCausaErrorDigitacion_whenCondicionOptimo() {
        Producto producto = crearProducto(300);
        LoteProducto lote = crearLote(600L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.ERROR_DIGITACION, CondicionProductoDevuelto.OPTIMO);

        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_ENTRADA_ID);
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = capturarMovimientoGuardado(dto);

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getCausaDevolucionPt())
                .isEqualTo(CausaDevolucionPT.ERROR_DIGITACION);
        assertThat(movimientoCaptor.getValue().getAlmacenDestino().getId()).isEqualTo((int) ALMACEN_PT_ID);
    }

    @Test
    void shouldFail_whenMissingDocReferenciaOrClienteNombre() {
        Producto producto = crearProducto(400);
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                null,
                "Observaciones",
                null,
                CausaDevolucionPT.TROCADO,
                CondicionProductoDevuelto.OPTIMO,
                producto.getId(),
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
                null,
                null,
                null,
                null,
                false,
                null
        );

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS));
    }

    private ArgumentCaptor<LoteProducto> capturarLoteGuardado(MovimientoInventarioDTO dto) {
        stubMovimientoPersistencia(dto);
        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());
        return loteCaptor;
    }

    private ArgumentCaptor<MovimientoInventario> capturarMovimientoGuardado(MovimientoInventarioDTO dto) {
        stubMovimientoPersistencia(dto);
        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        given(movimientoInventarioRepository.save(movimientoCaptor.capture())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(10L);
            return mov;
        });
        return movimientoCaptor;
    }

    private void stubMovimientoPersistencia(MovimientoInventarioDTO dto) {
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setCausaDevolucionPt(dto.causaDevolucionPt());
        movimientoEntidad.setCondicionProductoDevuelto(dto.condicionProductoDevuelto());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(10L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(10L);
            return mov;
        });
    }

    private void configurarMocksBasicos(Producto producto, LoteProducto lote, long almacenDestinoId, long tipoDetalleId) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleId);
        given(tipoMovimientoDetalleRepository.findById(tipoDetalleId)).willReturn(Optional.of(tipoDetalle));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                lote.getCodigoLote(), producto.getId(), (int) almacenDestinoId))
                .willReturn(Optional.empty());
        given(entityManager.getReference(eq(Almacen.class), any()))
                .willAnswer(invocation -> new Almacen(((Number) invocation.getArgument(1)).intValue()));
    }

    private MovimientoInventarioDTO construirDto(Integer productoId,
                                                 Long loteProductoId,
                                                 CausaDevolucionPT causa,
                                                 CondicionProductoDevuelto condicion) {
        return new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                "DOC-DEV-1",
                "Observaciones",
                "Cliente Uno",
                causa,
                condicion,
                productoId,
                loteProductoId,
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
                null,
                null,
                null,
                false,
                null
        );
    }

    private Producto crearProducto(Integer id) {
        Producto producto = new Producto();
        producto.setId(id);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(10L);
        producto.setUnidadMedida(unidad);
        return producto;
    }

    private LoteProducto crearLote(Long id, Producto producto, int almacenId, EstadoLote estado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setCodigoLote("LOTE-" + id);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(estado);
        lote.setStockLote(new BigDecimal("5"));
        lote.setStockReservado(BigDecimal.ZERO);
        return lote;
    }

    private void stubCatalogosRecepcionDevolucion() {
        MotivoMovimiento motivoEntradaPt = new MotivoMovimiento();
        motivoEntradaPt.setId(MOTIVO_ENTRADA_PT_ID);
        motivoEntradaPt.setMotivo(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO);
        MotivoMovimiento motivoTransferenciaCalidad = new MotivoMovimiento();
        motivoTransferenciaCalidad.setId(MOTIVO_TRANSFERENCIA_CALIDAD_ID);
        motivoTransferenciaCalidad.setMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);

        lenient().when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(MOTIVO_ENTRADA_PT_ID);
        lenient().when(motivoMovimientoRepository.findById(MOTIVO_ENTRADA_PT_ID))
                .thenReturn(Optional.of(motivoEntradaPt));
        lenient().when(catalogResolver.getMotivoIdTransferenciaCalidad())
                .thenReturn(MOTIVO_TRANSFERENCIA_CALIDAD_ID);
        lenient().when(motivoMovimientoRepository.findById(MOTIVO_TRANSFERENCIA_CALIDAD_ID))
                .thenReturn(Optional.of(motivoTransferenciaCalidad));
        lenient().when(catalogResolver.getAlmacenPtId()).thenReturn(ALMACEN_PT_ID);
        lenient().when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(ALMACEN_CUARENTENA_ID);
        lenient().when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(TIPO_DETALLE_ENTRADA_ID);
        lenient().when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(TIPO_DETALLE_TRANSFERENCIA_ID);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
    }

    private void stubPersistenciaLoteDestino() {
        lenient().doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).save(any(LoteProducto.class));

        lenient().doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).saveAndFlush(any(LoteProducto.class));
    }
}
