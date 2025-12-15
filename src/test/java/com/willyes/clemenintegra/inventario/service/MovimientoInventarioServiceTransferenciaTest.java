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
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceTransferenciaTest {

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
    void transferenciaConLoteLiberadoEnOrigenSeAcepta() {
        Producto producto = crearProducto(8, 2);
        LoteProducto lote = crearLote(30L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());

        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        given(movimientoInventarioRepository.save(any(MovimientoInventario.class))).willAnswer(invocation -> {
            MovimientoInventario mov = invocation.getArgument(0);
            mov.setId(200L);
            return mov;
        });
        given(mapper.safeToResponseDTO(any(MovimientoInventario.class)))
                .willReturn(MovimientoInventarioResponseDTO.builder().id(200L).build());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getId()).isEqualTo(200L);
        assertThat(lote.getStockLote()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void transferenciaConLoteEnCuarentenaDevuelveErrorDeCalidad() {
        Producto producto = crearProducto(18, 2);
        LoteProducto lote = crearLote(301L, producto, 1, EstadoLote.EN_CUARENTENA,
                new BigDecimal("4000"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("500"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null
        );

        configurarMocksBasicos(producto, lote);
        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setFechaIngreso(LocalDateTime.now());
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        movimientoEntidad.setCantidad(dto.cantidad());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);
        doThrow(new CustomBusinessException(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO,
                "Lote no liberado"))
                .when(loteCalidadValidator).validarLoteUtilizable(lote);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CALIDAD_LOTE_NO_LIBERADO));
    }

    @Test
    void transferenciaConStockInsuficienteDisparaError() {
        Producto producto = crearProducto(9, 2);
        LoteProducto lote = crearLote(31L, producto, 1, EstadoLote.LIBERADO,
                new BigDecimal("500"), BigDecimal.ZERO, false);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("1000"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                null,
                producto.getId(),
                lote.getId(),
                1,
                6,
                null,
                null,
                null,
                5L,
                null,
                null,
                null,
                null,
                lote.getCodigoLote(),
                null,
                null,
                Boolean.FALSE,
                null
        );

        configurarMocksBasicos(producto, lote);

        MovimientoInventario movimientoEntidad = new MovimientoInventario();
        movimientoEntidad.setTipoMovimiento(dto.tipoMovimiento());
        movimientoEntidad.setClasificacion(dto.clasificacionMovimientoInventario());
        given(mapper.toEntity(dto)).willReturn(movimientoEntidad);

        assertThatThrownBy(() -> service.registrarMovimiento(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LOTE_NO_DISPONIBLE_TRANSFERIR");
    }

    private void configurarMocksBasicos(Producto producto, LoteProducto lote) {
        given(productoRepository.findById(producto.getId().longValue())).willReturn(Optional.of(producto));
        given(tipoMovimientoDetalleRepository.findById(5L)).willReturn(Optional.of(new TipoMovimientoDetalle()));
        given(loteProductoRepository.findByIdForUpdate(lote.getId())).willReturn(Optional.of(lote));
        lenient().when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                lote.getCodigoLote(), producto.getId(), 6)).thenReturn(Optional.empty());
        lenient().when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto loteArgument = invocation.getArgument(0);
            if (loteArgument.getId() == null) {
                loteArgument.setId(900L);
            }
            return loteArgument;
        });
        lenient().when(entityManager.getReference(eq(Almacen.class), any()))
                .thenAnswer(invocation -> {
                    Object id = invocation.getArgument(1);
                    return new Almacen(id instanceof Integer ? (Integer) id : ((Long) id).intValue());
                });
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
    }

    private Producto crearProducto(Integer id, int escala) {
        Producto producto = new Producto();
        producto.setId(id);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(100L);
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
        lote.setCodigoLote("L-" + id);
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(estado);
        lote.setStockLote(stock);
        lote.setStockReservado(reservado);
        lote.setAgotado(agotado);
        return lote;
    }
}
