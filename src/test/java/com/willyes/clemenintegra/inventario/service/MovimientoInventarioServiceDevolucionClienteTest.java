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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceDevolucionClienteTest {

    private static final long ALMACEN_PT_ID = 2L;
    private static final long ALMACEN_CUARENTENA_ID = 7L;
    private static final long TIPO_DETALLE_ENTRADA_ID = 15L;
    private static final long TIPO_DETALLE_EXPLICITO_ID = 18L;

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

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setupSecurity() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tester", "secret");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        lenient().when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(
                Usuario.builder().id(1L).nombreCompleto("Tester").build()
        );
    }

    @Test
    void shouldRouteToBodegaPt_whenTrocadoAndOptimo_andNotLegacy() {
        Producto producto = crearProducto(100);
        LoteProducto lote = crearLote(400L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID);

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
        given(loteProductoRepository.save(loteCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_PT_ID);
    }

    @Test
    void shouldRouteToCuarentena_whenCortaFecha_or_CondicionNotOptimo() {
        Producto producto = crearProducto(200);
        LoteProducto lote = crearLote(500L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.CORTA_FECHA, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID);

        configurarMocksBasicos(producto, lote, ALMACEN_CUARENTENA_ID, TIPO_DETALLE_EXPLICITO_ID);

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
        given(loteProductoRepository.save(loteCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        service.registrarMovimiento(dto);

        assertThat(loteCaptor.getValue().getAlmacen().getId()).isEqualTo((int) ALMACEN_CUARENTENA_ID);
        assertThat(loteCaptor.getValue().getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
    }

    @Test
    void shouldReject_whenNotLegacy_andMissingLoteProductoId() {
        Producto producto = crearProducto(300);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), null,
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID);

        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.DEVOLUCION_PT_LOTE_REQUERIDO);
                    assertThat((Map<String, Object>) ex.getDetails())
                            .containsEntry("productoId", producto.getId());
                });
    }

    @Test
    void shouldAssignTipoDetalleEntrada_whenMissingTipoDetalleId() {
        Producto producto = crearProducto(400);
        LoteProducto lote = crearLote(600L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, null);

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

        service.registrarMovimiento(dto);

        assertThat(movimientoCaptor.getValue().getTipoMovimientoDetalle().getId())
                .isEqualTo(TIPO_DETALLE_ENTRADA_ID);
    }

    @Test
    void shouldRespectExplicitTipoDetalleId_whenProvided() {
        Producto producto = crearProducto(500);
        LoteProducto lote = crearLote(700L, producto, 2, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(),
                CausaDevolucionPT.TROCADO, CondicionProductoDevuelto.OPTIMO, false, TIPO_DETALLE_EXPLICITO_ID);

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

    private void configurarMocksBasicos(Producto producto, LoteProducto lote, long almacenDestinoId, long tipoDetalleId) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(1L);
        motivo.setMotivo(ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE);

        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleId);
        given(tipoMovimientoDetalleRepository.findById(tipoDetalleId)).willReturn(Optional.of(tipoDetalle));
        given(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE))
                .willReturn(Optional.of(motivo));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                lote.getCodigoLote(), producto.getId(), (int) almacenDestinoId))
                .willReturn(Optional.empty());
        given(entityManager.getReference(eq(Almacen.class), any()))
                .willAnswer(invocation -> new Almacen(((Number) invocation.getArgument(1)).intValue()));
        given(catalogResolver.getAlmacenPtId()).willReturn(ALMACEN_PT_ID);
        given(catalogResolver.getAlmacenCuarentenaId()).willReturn(ALMACEN_CUARENTENA_ID);
        lenient().when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(TIPO_DETALLE_ENTRADA_ID);
        given(catalogResolver.decimals(any())).willReturn(2);
        lenient().when(catalogResolver.isSalidaPtEnabled()).thenReturn(false);
        lenient().when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(TIPO_DETALLE_ENTRADA_ID);
        lenient().when(catalogResolver.getTipoDetalleSalidaPtId()).thenReturn(TIPO_DETALLE_ENTRADA_ID);
    }

    private MovimientoInventarioDTO construirDto(Integer productoId,
                                                 Long loteProductoId,
                                                 CausaDevolucionPT causa,
                                                 CondicionProductoDevuelto condicion,
                                                 boolean loteLegacy,
                                                 Long tipoDetalleId) {
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
}
