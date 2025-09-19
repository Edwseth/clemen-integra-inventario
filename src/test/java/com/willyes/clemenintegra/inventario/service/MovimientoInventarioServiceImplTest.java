package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceImplTest {

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
    private MovimientoInventarioMapper movimientoInventarioMapper;
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
    private EntityManager entityManager;

    private MovimientoInventarioServiceImpl service;
    private Usuario usuarioOperador;

    @BeforeEach
    void setUp() {
        service = new MovimientoInventarioServiceImpl(
                almacenRepository,
                productoRepository,
                proveedorRepository,
                ordenCompraRepository,
                ordenCompraService,
                loteProductoRepository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                movimientoInventarioRepository,
                movimientoInventarioMapper,
                usuarioService,
                solicitudMovimientoRepository,
                solicitudMovimientoDetalleRepository,
                catalogResolver,
                reservaLoteService,
                entityManager
        );
        usuarioOperador = new Usuario();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuarioOperador);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("tester", "secret"));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferenciaManualDesdeCuarentenaAceptaLoteLiberadoProductoTerminado() {
        Integer productoId = 100;
        Long loteId = 50L;
        Integer origenId = 1;
        Integer destinoId = 2;
        Long tipoDetalleId = 10L;
        BigDecimal cantidad = new BigDecimal("5.0");

        CategoriaProducto categoriaPT = CategoriaProducto.builder()
                .id(1L)
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build();
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setCategoriaProducto(categoriaPT);

        Almacen almacenOrigen = Almacen.builder()
                .id(origenId)
                .nombre("Cuarentena")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();
        Almacen almacenDestino = Almacen.builder()
                .id(destinoId)
                .nombre("Principal")
                .ubicacion("Zona PT")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();

        LocalDateTime fechaLiberacion = LocalDateTime.now().minusHours(2);
        Usuario usuarioLiberador = new Usuario();

        LoteProducto loteOrigen = LoteProducto.builder()
                .id(loteId)
                .codigoLote("L-001")
                .producto(producto)
                .almacen(almacenOrigen)
                .estado(EstadoLote.LIBERADO)
                .stockLote(new BigDecimal("10.0"))
                .stockReservado(BigDecimal.ZERO)
                .fechaLiberacion(fechaLiberacion)
                .usuarioLiberador(usuarioLiberador)
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                cantidad,
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                productoId,
                loteId,
                origenId,
                destinoId,
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
                Boolean.FALSE,
                null
        );

        TipoMovimientoDetalle tipoDetalle = TipoMovimientoDetalle.builder()
                .id(tipoDetalleId)
                .descripcion("Transferencia manual")
                .build();
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);

        configurarStubsBasicos(dto, producto, loteOrigen, almacenOrigen, almacenDestino, tipoDetalle, movimiento);
        when(loteProductoRepository.findByProductoIdAndCodigoLoteAndAlmacenIdForUpdate(
                eq(productoId),
                eq(loteOrigen.getCodigoLote()),
                eq(destinoId))
        ).thenReturn(Optional.empty());
        when(loteProductoRepository.findByCodigoLoteAndProductoId(
                eq(loteOrigen.getCodigoLote()),
                eq(productoId.longValue())
        )).thenReturn(Optional.of(loteOrigen));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(inv -> {
                    MovimientoInventario guardado = inv.getArgument(0);
                    guardado.setId(200L);
                    return guardado;
                });
        when(movimientoInventarioMapper.safeToResponseDTO(any()))
                .thenReturn(new MovimientoInventarioResponseDTO());

        MovimientoInventarioResponseDTO respuesta = service.registrarMovimiento(dto);

        assertNotNull(respuesta);

        ArgumentCaptor<LoteProducto> loteCaptor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository, atLeast(2)).save(loteCaptor.capture());
        List<LoteProducto> guardados = loteCaptor.getAllValues();
        LoteProducto loteDestinoGuardado = guardados.stream()
                .filter(l -> l.getAlmacen() != null
                        && Objects.equals(l.getAlmacen().getId(), destinoId))
                .reduce((first, second) -> second)
                .orElse(null);

        assertNotNull(loteDestinoGuardado, "Se esperaba registrar lote destino");
        assertEquals(EstadoLote.DISPONIBLE, loteDestinoGuardado.getEstado());
        assertEquals(fechaLiberacion, loteDestinoGuardado.getFechaLiberacion());
        assertSame(usuarioLiberador, loteDestinoGuardado.getUsuarioLiberador());
        assertEquals(new BigDecimal("5.00"), loteDestinoGuardado.getStockLote());
    }

    @Test
    void transferenciaManualDesdeCuarentenaRechazaEstadosNoPermitidos() {
        Integer productoId = 101;
        Long loteId = 60L;
        Integer origenId = 11;
        Integer destinoId = 12;
        Long tipoDetalleId = 20L;

        CategoriaProducto categoriaPT = CategoriaProducto.builder()
                .id(2L)
                .nombre("PT")
                .tipo(TipoCategoria.PRODUCTO_TERMINADO)
                .build();
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setCategoriaProducto(categoriaPT);

        Almacen almacenOrigen = Almacen.builder()
                .id(origenId)
                .nombre("Cuarentena")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();
        Almacen almacenDestino = Almacen.builder()
                .id(destinoId)
                .nombre("Principal")
                .ubicacion("Zona PT")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();

        LoteProducto loteOrigen = LoteProducto.builder()
                .id(loteId)
                .codigoLote("L-002")
                .producto(producto)
                .almacen(almacenOrigen)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(new BigDecimal("8.0"))
                .stockReservado(BigDecimal.ZERO)
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("3.0"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                productoId,
                loteId,
                origenId,
                destinoId,
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
                Boolean.FALSE,
                null
        );

        TipoMovimientoDetalle tipoDetalle = TipoMovimientoDetalle.builder()
                .id(tipoDetalleId)
                .descripcion("Transferencia manual")
                .build();
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);

        configurarStubsBasicos(dto, producto, loteOrigen, almacenOrigen, almacenDestino, tipoDetalle, movimiento);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarMovimiento(dto));
        assertEquals("LOTE_NO_DISPONIBLE_TRANSFERIR", ex.getReason());
    }

    @Test
    void transferenciaManualDesdeCuarentenaRechazaProductoNoTerminado() {
        Integer productoId = 102;
        Long loteId = 61L;
        Integer origenId = 13;
        Integer destinoId = 14;
        Long tipoDetalleId = 21L;

        CategoriaProducto categoriaMP = CategoriaProducto.builder()
                .id(3L)
                .nombre("MP")
                .tipo(TipoCategoria.MATERIA_PRIMA)
                .build();
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setCategoriaProducto(categoriaMP);

        Almacen almacenOrigen = Almacen.builder()
                .id(origenId)
                .nombre("Cuarentena")
                .ubicacion("Zona QA")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();
        Almacen almacenDestino = Almacen.builder()
                .id(destinoId)
                .nombre("Principal")
                .ubicacion("Zona PT")
                .categoria(TipoCategoria.MATERIA_PRIMA)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();

        LoteProducto loteOrigen = LoteProducto.builder()
                .id(loteId)
                .codigoLote("L-003")
                .producto(producto)
                .almacen(almacenOrigen)
                .estado(EstadoLote.LIBERADO)
                .stockLote(new BigDecimal("6.0"))
                .stockReservado(BigDecimal.ZERO)
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null,
                new BigDecimal("2.0"),
                TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL,
                null,
                productoId,
                loteId,
                origenId,
                destinoId,
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
                Boolean.FALSE,
                null
        );

        TipoMovimientoDetalle tipoDetalle = TipoMovimientoDetalle.builder()
                .id(tipoDetalleId)
                .descripcion("Transferencia manual")
                .build();
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL);

        configurarStubsBasicos(dto, producto, loteOrigen, almacenOrigen, almacenDestino, tipoDetalle, movimiento);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarMovimiento(dto));
        assertEquals("LOTE_NO_DISPONIBLE_TRANSFERIR", ex.getReason());
    }

    private void configurarStubsBasicos(MovimientoInventarioDTO dto,
                                        Producto producto,
                                        LoteProducto loteOrigen,
                                        Almacen almacenOrigen,
                                        Almacen almacenDestino,
                                        TipoMovimientoDetalle tipoDetalle,
                                        MovimientoInventario movimiento) {
        when(tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId()))
                .thenReturn(Optional.of(tipoDetalle));
        when(productoRepository.findById(dto.productoId().longValue()))
                .thenReturn(Optional.of(producto));
        when(entityManager.getReference(eq(Almacen.class), eq(dto.almacenOrigenId())))
                .thenReturn(almacenOrigen);
        when(entityManager.getReference(eq(Almacen.class), eq(dto.almacenDestinoId())))
                .thenReturn(almacenDestino);
        when(entityManager.getReference(eq(Almacen.class), eq(dto.almacenDestinoId().longValue())))
                .thenReturn(almacenDestino);
        when(entityManager.getReference(eq(Producto.class), eq(producto.getId().longValue())))
                .thenReturn(producto);
        when(movimientoInventarioMapper.toEntity(dto)).thenReturn(movimiento);
        when(loteProductoRepository.findByIdForUpdate(dto.loteProductoId()))
                .thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.save(any(LoteProducto.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioMapper.safeToResponseDTO(any()))
                .thenReturn(new MovimientoInventarioResponseDTO());
    }
}

