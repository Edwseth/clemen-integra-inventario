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
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceCorreccionSalidaClienteTest {

    private static final long TIPO_DETALLE_ID = 31L;
    private static final long MOTIVO_CORRECCION_ID = 80L;
    private static final int ALMACEN_DESTINO_ID = 2;

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
    }

    @Test
    void shouldRegisterCorreccionSalidaCliente_andIncreaseStock() {
        Producto producto = crearProducto(10);
        LoteProducto lote = crearLote(100L, producto, ALMACEN_DESTINO_ID, EstadoLote.LIBERADO);
        MovimientoInventarioDTO dto = construirDto(producto.getId(), lote.getId(), "SALIDA-123",
                "Observaciones válidas", new BigDecimal("3"));

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_ID);
        tipoDetalle.setDescripcion("CORRECCION");
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_ID)).willReturn(Optional.of(tipoDetalle));
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        given(entityManager.getReference(eq(Almacen.class), any()))
                .willAnswer(invocation -> new Almacen(((Number) invocation.getArgument(1)).intValue()));

        MotivoMovimiento motivo = MotivoMovimiento.builder()
                .id(MOTIVO_CORRECCION_ID)
                .motivo(ClasificacionMovimientoInventario.CORRECCION_SALIDA_CLIENTE)
                .descripcion("Corrección operativa de salida a cliente")
                .build();
        given(motivoMovimientoRepository.findById(MOTIVO_CORRECCION_ID)).willReturn(Optional.of(motivo));

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
        doNothing().when(loteCalidadValidator).validarLoteUtilizable(any(LoteProducto.class));

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        given(loteProductoRepository.save(loteCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(loteCaptor.getValue().getStockLote()).isEqualByComparingTo(new BigDecimal("8.000000"));
    }

    @Test
    void shouldFail_whenMissingDocReferencia() {
        MovimientoInventarioDTO dto = construirDto(10, 100L, null, "Observaciones válidas",
                new BigDecimal("3"));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CORRECCION_SALIDA_CLIENTE_DOC_REFERENCIA_REQUERIDA");
    }

    @Test
    void shouldFail_whenMissingObservaciones() {
        MovimientoInventarioDTO dto = construirDto(10, 100L, "SALIDA-123", "corta",
                new BigDecimal("3"));

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.OBSERVACION_REQUERIDA);
                    assertThat((Map<String, Object>) ex.getDetails()).containsEntry("minLength", 10);
                });
    }

    @Test
    void shouldFail_whenCantidadInvalida() {
        MovimientoInventarioDTO dto = construirDto(10, 100L, "SALIDA-123", "Observaciones válidas",
                BigDecimal.ZERO);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CORRECCION_SALIDA_CLIENTE_CANTIDAD_INVALIDA");
    }

    private MovimientoInventarioDTO construirDto(Integer productoId,
                                                 Long loteId,
                                                 String docReferencia,
                                                 String observaciones,
                                                 BigDecimal cantidad) {
        return new MovimientoInventarioDTO(
                null,
                cantidad,
                TipoMovimiento.RECEPCION,
                ClasificacionMovimientoInventario.CORRECCION_SALIDA_CLIENTE,
                docReferencia,
                observaciones,
                null,
                null,
                null,
                productoId,
                loteId,
                null,
                ALMACEN_DESTINO_ID,
                null,
                null,
                MOTIVO_CORRECCION_ID,
                TIPO_DETALLE_ID,
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
                null
        );
    }

    private Producto crearProducto(Integer id) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("Unidad");
        unidad.setSimbolo("UND");
        Producto producto = new Producto();
        producto.setId(id);
        producto.setUnidadMedida(unidad);
        return producto;
    }

    private LoteProducto crearLote(Long id, Producto producto, int almacenId, EstadoLote estado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setEstado(estado);
        lote.setStockLote(new BigDecimal("5.000000"));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setCodigoLote("L-001");
        return lote;
    }
}
