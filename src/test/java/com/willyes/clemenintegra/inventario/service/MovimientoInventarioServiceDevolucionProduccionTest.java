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
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.UbicacionFisicaRepository;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoInventarioServiceDevolucionProduccionTest {

    private static final long PRE_BODEGA_ID = 6L;
    private static final long PRINCIPAL_EMPAQUE_ID = 5L;
    private static final long TIPO_DETALLE_ID = 31L;
    private static final long MOTIVO_DEVOLUCION_ID = 41L;

    @Mock private AlmacenRepository almacenRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private OrdenCompraService ordenCompraService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private MovimientoInventarioMapper mapper;
    @Mock private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private ReservaLoteRepository reservaLoteRepository;
    @Mock private RecepcionOCService recepcionOCService;
    @Mock private LoteCalidadValidator loteCalidadValidator;
    @Mock private EntityManager entityManager;
    @Mock private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Mock private EtapaProduccionRepository etapaProduccionRepository;
    @Mock private CosteoInventarioService costeoInventarioService;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("tester", "secret");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        given(usuarioService.obtenerUsuarioAutenticado())
                .willReturn(Usuario.builder().id(1L).nombreCompleto("Tester").build());

        lenient().when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(99L);
        lenient().when(catalogResolver.decimals(any())).thenReturn(0);
        lenient().when(costeoInventarioService.calcularCostoTotalMovimiento(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(6));

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(TIPO_DETALLE_ID);
        lenient().when(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_ID))
                .thenReturn(Optional.of(tipoDetalle));

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(MOTIVO_DEVOLUCION_ID);
        motivo.setMotivo(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION);
        lenient().when(motivoMovimientoRepository.findById(MOTIVO_DEVOLUCION_ID))
                .thenReturn(Optional.of(motivo));
        lenient().when(etapaProduccionRepository.findById(33L))
                .thenReturn(Optional.of(crearEtapaActiva(33L, 277L)));

        lenient().when(entityManager.getReference(eq(Almacen.class), any()))
                .thenAnswer(invocation -> new Almacen(((Number) invocation.getArgument(1)).intValue()));

        lenient().doAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            if (lote.getId() == null) {
                lote.setId(9000L);
            }
            return lote;
        }).when(loteProductoRepository).save(any(LoteProducto.class));

        lenient().doAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            if (lote.getId() == null) {
                lote.setId(9000L);
            }
            return lote;
        }).when(loteProductoRepository).saveAndFlush(any(LoteProducto.class));

        lenient().when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    if (mov.getId() == null) {
                        mov.setId(4641L);
                    }
                    return mov;
                });
        lenient().when(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(4641L).build());
    }

    @Test
    void devolucionDesdeProduccion_acreditaMismoLoteDestino_existente_ME0114() {
        Producto producto = crearProducto(114, "ME0114");
        LoteProducto loteOrigen = crearLote(2492L, producto, "02E-0001", PRE_BODEGA_ID, "23", EstadoLote.LIBERADO, false);
        LoteProducto loteDestino = crearLote(2048L, producto, "02E-0001", PRINCIPAL_EMPAQUE_ID, "1842", EstadoLote.LIBERADO, false);

        stubMovimientoBase(producto);
        given(loteProductoRepository.findByIdForUpdate(2492L)).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(producto.getId(), "02E-0001", (int) PRINCIPAL_EMPAQUE_ID))
                .willReturn(Optional.of(loteDestino));

        MovimientoInventarioResponseDTO response = service.registrarMovimiento(construirDto(producto.getId(), 2492L, "23"));

        assertThat(response.getId()).isEqualTo(4641L);
        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo("0");
        assertThat(loteOrigen.isAgotado()).isTrue();
        assertThat(loteDestino.getId()).isEqualTo(2048L);
        assertThat(loteDestino.getStockLote()).isEqualByComparingTo("1865");
        assertThat(loteDestino.isAgotado()).isFalse();

        ArgumentCaptor<MovimientoInventario> movimientoCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getLote().getId()).isEqualTo(2048L);
    }

    @Test
    void devolucionDesdeProduccion_mantieneNoRegresion_ME0105_sobreLoteExistente() {
        Producto producto = crearProducto(105, "ME0105");
        LoteProducto loteOrigen = crearLote(3105L, producto, "05A-0007", PRE_BODEGA_ID, "10", EstadoLote.LIBERADO, false);
        LoteProducto loteDestino = crearLote(2105L, producto, "05A-0007", PRINCIPAL_EMPAQUE_ID, "50", EstadoLote.LIBERADO, false);

        stubMovimientoBase(producto);
        given(loteProductoRepository.findByIdForUpdate(3105L)).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(producto.getId(), "05A-0007", (int) PRINCIPAL_EMPAQUE_ID))
                .willReturn(Optional.of(loteDestino));

        service.registrarMovimiento(construirDto(producto.getId(), 3105L, "10"));

        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo("0");
        assertThat(loteDestino.getId()).isEqualTo(2105L);
        assertThat(loteDestino.getStockLote()).isEqualByComparingTo("60");
    }

    @Test
    void devolucionDesdeProduccion_sincronizaCostoEnDestinoExistenteCuandoEstaIncompleto() {
        Producto producto = crearProducto(333, "ME0333");
        LoteProducto loteOrigen = crearLote(4333L, producto, "03A-0003", PRE_BODEGA_ID, "5", EstadoLote.LIBERADO, false);
        loteOrigen.setCostoUnitarioMaterial(new BigDecimal("11.500000"));
        loteOrigen.setCostoTotalMaterialIngresado(new BigDecimal("57.500000"));
        loteOrigen.setTotalIngresadoMaterial(new BigDecimal("5.000000"));
        LoteProducto loteDestino = crearLote(2333L, producto, "03A-0003", PRINCIPAL_EMPAQUE_ID, "10", EstadoLote.LIBERADO, false);
        loteDestino.setCostoUnitarioMaterial(null);
        loteDestino.setCostoTotalMaterialIngresado(BigDecimal.ZERO.setScale(6));
        loteDestino.setTotalIngresadoMaterial(null);

        stubMovimientoBase(producto);
        given(loteProductoRepository.findByIdForUpdate(4333L)).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(producto.getId(), "03A-0003", (int) PRINCIPAL_EMPAQUE_ID))
                .willReturn(Optional.of(loteDestino));

        service.registrarMovimiento(construirDto(producto.getId(), 4333L, "5"));

        assertThat(loteDestino.getCostoUnitarioMaterial()).isEqualByComparingTo(new BigDecimal("11.500000"));
        assertThat(loteDestino.getCostoTotalMaterialIngresado()).isEqualByComparingTo(new BigDecimal("57.500000"));
        assertThat(loteDestino.getTotalIngresadoMaterial()).isEqualByComparingTo(new BigDecimal("5.000000"));
    }

    @Test
    void devolucionDesdeProduccion_creaLoteDestinoSiNoExiste_yLoAcredita() {
        Producto producto = crearProducto(222, "ME0999");
        LoteProducto loteOrigen = crearLote(3999L, producto, "09Z-0003", PRE_BODEGA_ID, "7", EstadoLote.LIBERADO, false);

        stubMovimientoBase(producto);
        given(loteProductoRepository.findByIdForUpdate(3999L)).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(producto.getId(), "09Z-0003", (int) PRINCIPAL_EMPAQUE_ID))
                .willReturn(Optional.empty());

        service.registrarMovimiento(construirDto(producto.getId(), 3999L, "7"));

        assertThat(loteOrigen.getStockLote()).isEqualByComparingTo("0");

        ArgumentCaptor<LoteProducto> saveCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository, never()).findByCodigoLoteAndProductoIdAndAlmacenId(any(), any(), any());
        verify(loteProductoRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(saveCaptor.capture());
        LoteProducto loteCreadoDestino = saveCaptor.getAllValues().stream()
                .filter(l -> l.getAlmacen() != null && Integer.valueOf((int) PRINCIPAL_EMPAQUE_ID).equals(l.getAlmacen().getId()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(loteCreadoDestino.getCodigoLote()).isEqualTo("09Z-0003");
        assertThat(loteCreadoDestino.getStockLote()).isEqualByComparingTo("7");
        assertThat(loteCreadoDestino.getCostoUnitarioMaterial()).isNull();
    }

    @Test
    void devolucionDesdeProduccion_creaLoteDestinoConContinuidadDeCostosDesdeOrigen() {
        Producto producto = crearProducto(444, "ME0444");
        LoteProducto loteOrigen = crearLote(4444L, producto, "04B-0004", PRE_BODEGA_ID, "6", EstadoLote.LIBERADO, false);
        loteOrigen.setCostoUnitarioMaterial(new BigDecimal("9.250000"));
        loteOrigen.setCostoTotalMaterialIngresado(new BigDecimal("55.500000"));
        loteOrigen.setTotalIngresadoMaterial(new BigDecimal("6.000000"));

        stubMovimientoBase(producto);
        given(loteProductoRepository.findByIdForUpdate(4444L)).willReturn(Optional.of(loteOrigen));
        given(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(producto.getId(), "04B-0004", (int) PRINCIPAL_EMPAQUE_ID))
                .willReturn(Optional.empty());

        service.registrarMovimiento(construirDto(producto.getId(), 4444L, "6"));

        ArgumentCaptor<LoteProducto> saveCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(saveCaptor.capture());
        LoteProducto loteCreadoDestino = saveCaptor.getAllValues().stream()
                .filter(l -> l.getAlmacen() != null && Integer.valueOf((int) PRINCIPAL_EMPAQUE_ID).equals(l.getAlmacen().getId()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(loteCreadoDestino.getCostoUnitarioMaterial()).isEqualByComparingTo(new BigDecimal("9.250000"));
        assertThat(loteCreadoDestino.getCostoTotalMaterialIngresado()).isEqualByComparingTo(new BigDecimal("55.500000"));
        assertThat(loteCreadoDestino.getTotalIngresadoMaterial()).isEqualByComparingTo(new BigDecimal("6.000000"));
    }

    private void stubMovimientoBase(Producto producto) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        MovimientoInventario mov = new MovimientoInventario();
        mov.setFechaIngreso(LocalDateTime.now());
        mov.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
        mov.setClasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION);
        given(mapper.toEntity(any(MovimientoInventarioDTO.class))).willReturn(mov);
    }

    private MovimientoInventarioDTO construirDto(Integer productoId, Long loteId, String cantidad) {
        return new MovimientoInventarioDTO(
                null,
                new BigDecimal(cantidad),
                TipoMovimiento.DEVOLUCION,
                ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION,
                "OP-TEST",
                null,
                null,
                null,
                null,
                productoId,
                loteId,
                (int) PRE_BODEGA_ID,
                (int) PRINCIPAL_EMPAQUE_ID,
                null,
                null,
                MOTIVO_DEVOLUCION_ID,
                TIPO_DETALLE_ID,
                null,
                1L,
                277L,
                33L,
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

    private Producto crearProducto(Integer id, String sku) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigoSku(sku);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(1L);
        producto.setUnidadMedida(unidad);
        return producto;
    }

    private LoteProducto crearLote(Long id,
                                   Producto producto,
                                   String codigoLote,
                                   long almacenId,
                                   String stock,
                                   EstadoLote estado,
                                   boolean agotado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setCodigoLote(codigoLote);
        lote.setAlmacen(new Almacen((int) almacenId));
        lote.setEstado(estado);
        lote.setStockLote(new BigDecimal(stock));
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setAgotado(agotado);
        return lote;
    }

    private EtapaProduccion crearEtapaActiva(Long etapaId, Long ordenProduccionId) {
        OrdenProduccion ordenProduccion = new OrdenProduccion();
        ordenProduccion.setId(ordenProduccionId);
        EtapaProduccion etapa = new EtapaProduccion();
        etapa.setId(etapaId);
        etapa.setOrdenProduccion(ordenProduccion);
        etapa.setEstado(EstadoEtapa.EN_PROCESO);
        etapa.setFechaInicio(LocalDateTime.now().minusHours(1));
        etapa.setFechaFin(null);
        return etapa;
    }
}
