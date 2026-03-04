package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.CausaDevolucionPT;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.CondicionProductoDevuelto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceDevolucionClienteTest {

    private static final long ALMACEN_PT_ID = 2L;
    private static final long ALMACEN_CUARENTENA_ID = 7L;
    private static final long TIPO_DETALLE_ENTRADA_ID = 15L;
    private static final long TIPO_DETALLE_TRANSFERENCIA_ID = 16L;
    private static final long TIPO_DETALLE_EXPLICITO_ID = 18L;
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
    @Mock
    private CosteoInventarioService costeoInventarioService;

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
        stubPersistenciaLoteDestino();
    }

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
    void shouldRouteToBodegaPt_whenCondicionOptimo() {
        Producto producto = crearProducto(100);
        LoteProducto lote = crearLote(400L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(10L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(10L);
            return mov;
        });

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_PT_ID);
    }

    @Test
    void shouldRouteToCuarentena_whenCondicionNotOptimo() {
        Producto producto = crearProducto(200);
        LoteProducto lote = crearLote(500L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.CORTA_FECHA, CondicionProductoDevuelto.DUDOSO, false, null, null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_CUARENTENA_ID, TIPO_DETALLE_TRANSFERENCIA_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(11L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(11L);
            return mov;
        });

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_CUARENTENA_ID);
        assertThat(loteCaptor.getValue().getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
    }

    @Test
    void shouldFailLegacyWithoutFechaVencimiento() {
        Producto producto = crearProducto(351);
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                "DOC-DEV-LEGACY",
                "Observaciones legacy",
                "Cliente Uno",
                CausaDevolucionPT.TROCADO,
                CondicionProductoDevuelto.OPTIMO,
                producto.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_EXPLICITO_ID,
                null,
                null,
                null,
                null,
                null,
                "L-LEG-1",
                null,
                null,
                null,
                null,
                true,
                null
        );

        stubCatalogosRecepcionDevolucion();
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_EXPLICITO_ID);
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_EXPLICITO_ID)).willReturn(Optional.of(tipoDetalle));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_REQUERIDA));
    }

    @Test
    void shouldFailNoLegacyWithoutFecha_whenLoteOrigenSinVencimiento() {
        Producto producto = crearProducto(352);
        LoteProducto lote = crearLote(652L, producto, 2, EstadoLote.LIBERADO);
        lote.setFechaVencimiento(null);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                null);

        stubCatalogosRecepcionDevolucion();
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_EXPLICITO_ID);
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_EXPLICITO_ID)).willReturn(Optional.of(tipoDetalle));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_REQUERIDA));
    }

    @Test
    void shouldReject_whenNotLegacy_andMissingLoteProductoId() {
        Producto producto = crearProducto(300);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), null,
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                null);

        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.DEVOLUCION_PT_LOTE_REQUERIDO);
                    assertThat((Map<String, Object>) ex.getDetails())
                            .containsEntry("productoId", producto.getId());
                });
    }

    @Test
    void shouldCreateLegacyLote_whenCodigoLoteProvided() {
        Producto producto = crearProducto(350);
        String codigoLote = "202613-DXY";
        MovimientoInventarioDTO dto = construirDtoLegacy(producto.getId(), codigoLote, CondicionProductoDevuelto.OPTIMO);

        stubCatalogosRecepcionDevolucion();
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_EXPLICITO_ID);
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_EXPLICITO_ID))
                .willReturn(Optional.of(tipoDetalle));
        given(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                codigoLote, producto.getId(), (int) ALMACEN_PT_ID))
                .willReturn(Optional.empty());
        given(entityManager.getReference(eq(Almacen.class), any()))
                .willAnswer(invocation -> new Almacen(((Number) invocation.getArgument(1)).intValue()));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(12L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(12L);
            return mov;
        });

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(1001L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());

        service.registrarMovimiento(dto);

        LoteProducto creado = loteCaptor.getValue();
        assertThat(creado.getCodigoLote()).isEqualTo(codigoLote);
        assertThat(creado.getEstado()).isNotNull();
        assertThat(creado.getStockLote()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldSetFechaVencimientoWhenNoLegacyAndOrigenSinFecha() {
        Producto producto = crearProducto(353);
        LoteProducto lote = crearLote(653L, producto, 2, EstadoLote.LIBERADO);
        lote.setFechaVencimiento(null);
        LocalDateTime fecha = LocalDateTime.now().plusDays(60);
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                "DOC-DEV-1",
                "Observaciones",
                "Cliente Uno",
                CausaDevolucionPT.TROCADO,
                CondicionProductoDevuelto.OPTIMO,
                producto.getId(),
                lote.getId(),
                null,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_EXPLICITO_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                fecha,
                null,
                null,
                null,
                false,
                null
        );

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(16L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(16L);
            return mov;
        });

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(1002L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getFechaVencimiento()).isEqualTo(fecha);
    }

    void shouldAssignTipoDetalleEntrada_whenMissingTipoDetalleId() {
        Producto producto = crearProducto(400);
        LoteProducto lote = crearLote(600L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, null, null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_ENTRADA_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(12L).build());

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        given(movimientoInventarioRepository.save(movimientoCaptor.capture())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(12L);
            return mov;
        });

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);

        doAnswer(invocation -> {
            LoteProducto lp = invocation.getArgument(0);
            if (lp.getId() == null) {
                lp.setId(999L);
            }
            return lp;
        }).when(loteProductoRepository).save(loteCaptor.capture());

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getTipoMovimientoDetalle().getId())
                .isEqualTo(TIPO_DETALLE_ENTRADA_ID);
    }

    @Test
    void shouldAssignMotivoEntradaPt_whenDestinoPt() {
        Producto producto = crearProducto(410);
        LoteProducto lote = crearLote(610L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(14L).build());

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        given(movimientoInventarioRepository.save(movimientoCaptor.capture())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(14L);
            return mov;
        });

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getMotivoMovimiento().getId())
                .isEqualTo(MOTIVO_ENTRADA_PT_ID);
    }

    @Test
    void shouldAssignMotivoTransferenciaCalidad_whenDestinoCuarentena() {
        Producto producto = crearProducto(420);
        LoteProducto lote = crearLote(620L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.AVERIADO_TRANSPORTE, CondicionProductoDevuelto.AVERIADO, false,
                null, null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_CUARENTENA_ID, TIPO_DETALLE_TRANSFERENCIA_ID);
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(15L).build());

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        given(movimientoInventarioRepository.save(movimientoCaptor.capture())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(15L);
            return mov;
        });

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getMotivoMovimiento().getId())
                .isEqualTo(MOTIVO_TRANSFERENCIA_CALIDAD_ID);
        assertThat(movimientoCaptor.getValue().getTipoMovimientoDetalle().getId())
                .isEqualTo(TIPO_DETALLE_TRANSFERENCIA_ID);
    }

    @Test
    void shouldKeepFailing_whenMissingMotivoForOtherClasificacion() {
        Producto producto = crearProducto(430);
        MovimientoInventarioDTO dto = construirDtoRecepcionCompra(producto.getId());

        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectRecepcionCompraWhenOrdenEsServicios() {
        Producto producto = crearProducto(431);
        MovimientoInventarioDTO dto = construirDtoRecepcionCompra(producto.getId());

        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(motivoMovimientoRepository.findById(dto.motivoMovimientoId()))
                .willReturn(Optional.of(MotivoMovimiento.builder()
                        .id(dto.motivoMovimientoId())
                        .motivo(ClasificacionMovimientoInventario.RECEPCION_COMPRA)
                        .descripcion("Recepción compra")
                        .build()));
        given(ordenCompraRepository.findById(dto.ordenCompraId().longValue()))
                .willReturn(Optional.of(OrdenCompra.builder()
                        .id(dto.ordenCompraId())
                        .tipo(TipoOrdenCompra.SERVICIOS)
                        .build()));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> assertThat(((CustomBusinessException) ex).getCode())
                        .isEqualTo(ApiErrorCode.OC_SERVICIO_NO_RECEPCIONABLE));
    }

    @Test
    void shouldRespectExplicitTipoDetalleId_whenProvided() {
        Producto producto = crearProducto(500);
        LoteProducto lote = crearLote(700L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                null);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(13L).build());

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        given(movimientoInventarioRepository.save(movimientoCaptor.capture())).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(13L);
            return mov;
        });

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getTipoMovimientoDetalle().getId())
                .isEqualTo(TIPO_DETALLE_EXPLICITO_ID);
    }

    @Test
    void shouldFail_whenMotivoExplicitMissing() {
        Producto producto = crearProducto(510);
        LoteProducto lote = crearLote(710L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID,
                999L);

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);
        given(motivoMovimientoRepository.findById(999L)).willReturn(Optional.empty());

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CATALOGO_FALTANTE);
                    assertThat((Map<String, Object>) ex.getDetails())
                            .containsEntry("motivoMovimientoId", 999L);
                });
    }

    @Test
    void shouldFail_whenTipoDetalleExplicitMissing() {
        Producto producto = crearProducto(520);
        LoteProducto lote = crearLote(720L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, 999L, null);

        stubCatalogosRecepcionDevolucion();
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CATALOGO_FALTANTE);
                    assertThat((Map<String, Object>) ex.getDetails())
                            .containsEntry("tipoMovimientoDetalleId", 999L);
                });
    }


    @Test
    void shouldAcceptSameFechaVencimientoByDateIgnoringTime_forNonLegacy() {
        Producto producto = crearProducto(910);
        LoteProducto lote = crearLote(911L, producto, 2, EstadoLote.LIBERADO);
        lote.setFechaVencimiento(LocalDateTime.of(2026, 9, 2, 11, 45));

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                "DOC-DEV-ISO",
                "Observaciones",
                "Cliente Uno",
                CausaDevolucionPT.TROCADO,
                CondicionProductoDevuelto.OPTIMO,
                producto.getId(),
                lote.getId(),
                null,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_EXPLICITO_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 9, 2, 0, 0),
                null,
                null,
                null,
                false,
                null
        );

        stubCatalogosRecepcionDevolucion();
        configurarMocksBasicos(producto, lote, ALMACEN_PT_ID, TIPO_DETALLE_EXPLICITO_ID);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(212L).build());
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(212L);
            return mov;
        });

        MovimientoInventarioResponseDTO response = service.registrarMovimiento(dto);

        assertThat(response.getId()).isEqualTo(212L);
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
                                                 CondicionProductoDevuelto condicion,
                                                 boolean loteLegacy,
                                                 Long tipoDetalleId,
                                                 Long motivoMovimientoId) {
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
                motivoMovimientoId,
                tipoDetalleId,
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
                loteLegacy,
                null
        );
    }

    private MovimientoInventarioDTO construirDtoLegacy(Integer productoId,
                                                       String codigoLote,
                                                       CondicionProductoDevuelto condicion) {
        return new MovimientoInventarioDTO(
                null,
                new BigDecimal("10"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE,
                "DOC-DEV-LEGACY",
                "Observaciones legacy",
                "Cliente Uno",
                CausaDevolucionPT.TROCADO,
                condicion,
                productoId,
                null,
                null,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_EXPLICITO_ID,
                null,
                null,
                null,
                null,
                null,
                codigoLote,
                LocalDateTime.now().plusDays(120),
                null,
                null,
                null,
                true,
                null
        );
    }

    private MovimientoInventarioDTO construirDtoRecepcionCompra(Integer productoId) {
        return new MovimientoInventarioDTO(
                null,
                new BigDecimal("5"),
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.RECEPCION_COMPRA,
                "DOC-OC-1",
                "Destino",
                null,
                null,
                null,
                productoId,
                null,
                null,
                (int) ALMACEN_PT_ID,
                1,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now().plusDays(10),
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
