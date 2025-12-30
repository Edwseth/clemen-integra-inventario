package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceAlmacenOrigenTest {

    private static final long TIPO_DETALLE_SALIDA_PRODUCCION_ID = 70L;

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
        lenient().when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(null);
    }

    @Test
    void salidaProduccionValidaAlmacenContraLoteActual() {
        Producto producto = crearProducto(50, 2);
        LoteProducto loteOrigenPrincipal = crearLote(100L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("15"), BigDecimal.ZERO, false);
        LoteProducto lotePreBodega = crearLote(200L, producto, 6, EstadoLote.LIBERADO,
                new BigDecimal("10"), BigDecimal.ZERO, false);
        lotePreBodega.setLoteOrigen(loteOrigenPrincipal);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("3"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                producto.getId(),
                lotePreBodega.getId(),
                6,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_SALIDA_PRODUCCION_ID,
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

        configurarMocksBasicos(producto, lotePreBodega);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(500L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(500L).build());

        assertThatNoException().isThrownBy(() -> service.registrarMovimiento(dto));

        assertThat(lotePreBodega.getStockLote()).isEqualByComparingTo(new BigDecimal("7.00"));
        verify(movimientoInventarioRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void salidaProduccionNoSeNormalizaComoSalidaPt() {
        Producto producto = crearProducto(818, 2);
        LoteProducto lotePreBodega = crearLote(300L, producto, 6, EstadoLote.LIBERADO,
                new BigDecimal("5"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("2"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                null,
                null,
                producto.getId(),
                lotePreBodega.getId(),
                6,
                null,
                null,
                null,
                null,
                TIPO_DETALLE_SALIDA_PRODUCCION_ID,
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

        configurarMocksBasicos(producto, lotePreBodega);
        lenient().when(catalogResolver.isSalidaPtEnabled()).thenReturn(true);
        lenient().when(catalogResolver.getTipoDetalleSalidaPtId()).thenReturn(TIPO_DETALLE_SALIDA_PRODUCCION_ID);
        lenient().when(catalogResolver.getAlmacenPtId()).thenReturn(2L);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(501L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(501L).build());

        assertThatNoException().isThrownBy(() -> service.registrarMovimiento(dto));

        assertThat(lotePreBodega.getStockLote()).isEqualByComparingTo(new BigDecimal("3.00"));
        verify(movimientoInventarioRepository).save(any(MovimientoInventario.class));
    }

    private void configurarMocksBasicos(Producto producto, LoteProducto lotePreBodega) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_SALIDA_PRODUCCION_ID);
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_SALIDA_PRODUCCION_ID))
                .willReturn(Optional.of(tipoDetalle));
        given(loteProductoRepository.findByIdForUpdate(lotePreBodega.getId())).willReturn(Optional.of(lotePreBodega));
        lenient().when(entityManager.getReference(eq(Almacen.class), any()))
                .thenAnswer(invocation -> {
                    Object id = invocation.getArgument(1);
                    return new Almacen(id instanceof Integer ? (Integer) id : ((Long) id).intValue());
                });
        lenient().when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
    }

    private Producto crearProducto(Integer id, int escala) {
        Producto producto = new Producto();
        producto.setId(id);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(900L);
        producto.setUnidadMedida(unidad);
        lenient().when(catalogResolver.decimals(unidad)).thenReturn(escala);
        return producto;
    }

    private LoteProducto crearLote(Long id,
                                   Producto producto,
                                   Integer almacenId,
                                   EstadoLote estado,
                                   BigDecimal stock,
                                   BigDecimal reservado,
                                   boolean agotado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setCodigoLote("LOT-" + id);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(estado);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        lote.setAgotado(agotado);
        return lote;
    }
}
