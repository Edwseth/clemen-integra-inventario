package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceSalidaPtTest {

    private static final long TIPO_DETALLE_SALIDA_PT_ID = 20L;
    private static final long ALMACEN_PT_ID = 10L;

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
    void salidaPtAutoSplitConLoteNoLiberadoDevuelveErrorDeCalidad() {
        Producto producto = crearProducto(5, 2);
        LoteProducto lote = crearLote(100L, producto, ALMACEN_PT_ID, EstadoLote.EN_CUARENTENA,
                new BigDecimal("500"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("120"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_CLIENTE,
                "ORD-1",
                "Cliente demo",
                null,
                null,
                null,
                producto.getId(),
                null,
                Math.toIntExact(ALMACEN_PT_ID),
                null,
                null,
                null,
                70L,
                TIPO_DETALLE_SALIDA_PT_ID,
                null, // solicitudMovimientoId
                null, // usuarioId
                null, // ordenProduccionId
                null, // ordenProduccionEtapaId
                null, // ordenCompraDetalleId
                null, // codigoLote
                null, // fechaVencimiento
                null, // estadoLote
                true,
                null,
                null,
                null
        );

        configurarMocksSalidaPt(producto, lote);
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        doThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                "Lote no liberado",
                Map.of("loteId", lote.getId(), "estado", lote.getEstado().name())))
                .when(loteCalidadValidator).validarLoteUtilizable(any());

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO);
                    assertThat((Map<String, Object>) ex.getDetails())
                            .containsEntry("loteId", lote.getId())
                            .containsEntry("estado", EstadoLote.EN_CUARENTENA.name());
                });
    }

    @Test
    void salidaPtAutoSplitConLoteLiberadoPermiteConsumo() {
        Producto producto = crearProducto(6, 2);
        LoteProducto lote = crearLote(200L, producto, ALMACEN_PT_ID, EstadoLote.LIBERADO,
                new BigDecimal("800"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("300"),
                TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_CLIENTE,
                "ORD-2",
                "Cliente demo",
                null,
                null,
                null,
                producto.getId(),
                null,
                Math.toIntExact(ALMACEN_PT_ID),
                null,
                null,
                null,
                70L,
                TIPO_DETALLE_SALIDA_PT_ID,
                null, // solicitudMovimientoId
                null, // usuarioId
                null, // ordenProduccionId
                null, // ordenProduccionEtapaId
                null, // ordenCompraDetalleId
                null, // codigoLote
                null, // fechaVencimiento
                null, // estadoLote
                true,
                null,
                null,
                null
        );

        configurarMocksSalidaPt(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .willAnswer(invocation -> {
                    MovimientoInventario mov = invocation.getArgument(0);
                    mov.setId(500L);
                    return mov;
                });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(500L).build());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(500L);
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    private void configurarMocksSalidaPt(Producto producto, LoteProducto lote) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(TIPO_DETALLE_SALIDA_PT_ID))
                .willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(catalogResolver.isSalidaPtEnabled()).willReturn(true);
        given(catalogResolver.getTipoDetalleSalidaPtId()).willReturn(TIPO_DETALLE_SALIDA_PT_ID);
        lenient().when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(TIPO_DETALLE_SALIDA_PT_ID);
        given(catalogResolver.getAlmacenPtId()).willReturn(ALMACEN_PT_ID);
        lenient().when(catalogResolver.decimals(any(UnidadMedida.class))).thenReturn(2);
        given(loteProductoRepository.findFefoSalidaPt(producto.getId().longValue(), ALMACEN_PT_ID,
                EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO)))
                .willReturn(List.of(lote));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willAnswer(invocation -> {
            loteCalidadValidator.validarLoteUtilizable(lote);
            return Optional.of(lote);
        });
        lenient().when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(entityManager.getReference(eq(Almacen.class), any()))
                .thenAnswer(invocation -> new Almacen((Integer) invocation.getArgument(1)));
        lenient().when(motivoMovimientoRepository.findById(70L)).thenReturn(Optional.of(new MotivoMovimiento()));
    }

    private Producto crearProducto(Integer id, int escala) {
        Producto producto = new Producto();
        producto.setId(id);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(100L);
        producto.setUnidadMedida(unidad);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setCategoriaProducto(categoria);
        lenient().when(catalogResolver.decimals(unidad)).thenReturn(escala);
        return producto;
    }

    private LoteProducto crearLote(Long id,
                                   Producto producto,
                                   Long almacenId,
                                   EstadoLote estado,
                                   BigDecimal stock,
                                   BigDecimal reservado,
                                   boolean agotado) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setCodigoLote("L-" + id);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenId.intValue()));
        lote.setEstado(estado);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        lote.setAgotado(agotado);
        return lote;
    }
}
