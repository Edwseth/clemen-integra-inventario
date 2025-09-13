package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceImplTest {

    @Mock private AlmacenRepository almacenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private OrdenCompraService ordenCompraService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private EntityManager entityManager;
    @Mock private InventoryCatalogResolver catalogResolver;

    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MovimientoInventarioServiceImpl(
                almacenRepository,
                usuarioRepository,
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
                catalogResolver,
                entityManager
        );
    }

    @Test
    void transferenciaParcialAObsoletosMarcaLoteDestinoRechazado() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Almacen destino = Almacen.builder().id(2).categoria(TipoCategoria.OBSOLETOS).build();
        Producto producto = Producto.builder().id(1).build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("5"), TipoMovimiento.TRANSFERENCIA, null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), destino.getId(),
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId(
                loteOrigen.getCodigoLote(), producto.getId(), destino.getId())).thenReturn(Optional.empty());
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteProducto result = ReflectionTestUtils.invokeMethod(service, "procesarMovimientoConLoteExistente",
                dto, TipoMovimiento.TRANSFERENCIA, origen, destino, producto, new BigDecimal("5"), false, null);

        assertEquals(EstadoLote.RECHAZADO, result.getEstado());
        assertEquals(new BigDecimal("5"), result.getStockLote());
    }

    @Test
    void transferenciaCompletaAObsoletosMarcaLoteRechazado() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Almacen destino = Almacen.builder().id(2).categoria(TipoCategoria.OBSOLETOS).build();
        Producto producto = Producto.builder().id(1).build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("10"), TipoMovimiento.TRANSFERENCIA, null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), destino.getId(),
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteProducto result = ReflectionTestUtils.invokeMethod(service, "procesarMovimientoConLoteExistente",
                dto, TipoMovimiento.TRANSFERENCIA, origen, destino, producto, new BigDecimal("10"), false, null);

        assertSame(loteOrigen, result);
        assertEquals(EstadoLote.RECHAZADO, result.getEstado());
        assertEquals(destino, result.getAlmacen());
    }

    @Test
    void loteEnCuarentenaConProductoTerminadoPermiteMovimiento() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder()
                .id(1)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.PRODUCTO_TERMINADO).build())
                .build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(new BigDecimal("10"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("1"), TipoMovimiento.SALIDA, null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,
                "procesarMovimientoConLoteExistente", dto, TipoMovimiento.SALIDA,
                origen, null, producto, new BigDecimal("1"), false, null));
    }

    @Test
    void loteEnCuarentenaConProductoNoTerminadoLanza422() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder()
                .id(1)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.MATERIA_PRIMA).build())
                .build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(new BigDecimal("10"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("1"), TipoMovimiento.SALIDA, null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ReflectionTestUtils.invokeMethod(service,
                        "procesarMovimientoConLoteExistente", dto, TipoMovimiento.SALIDA,
                        origen, null, producto, new BigDecimal("1"), false, null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void loteConEstadoRetenidoLanza422() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder()
                .id(1)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.PRODUCTO_TERMINADO).build())
                .build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.RETENIDO)
                .stockLote(new BigDecimal("10"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("1"), TipoMovimiento.SALIDA, null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ReflectionTestUtils.invokeMethod(service,
                        "procesarMovimientoConLoteExistente", dto, TipoMovimiento.SALIDA,
                        origen, null, producto, new BigDecimal("1"), false, null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void salidaProduccionDescuentaStock() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass"));

        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder().id(1)
                .categoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto()).build();
        LoteProducto lote = LoteProducto.builder()
                .id(100L)
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .build();
        Usuario usuario = new Usuario(); usuario.setId(1L);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("3"), TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION, null,
                producto.getId(), lote.getId(), origen.getId(), null,
                null, null, null, 1L, 1L, null,
                usuario.getId(), null, null, null, null);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(entityManager.getReference(Almacen.class, origen.getId())).thenReturn(origen);
        when(entityManager.getReference(Usuario.class, usuario.getId())).thenReturn(usuario);
        when(entityManager.getReference(TipoMovimientoDetalle.class, 1L)).thenReturn(new TipoMovimientoDetalle());
        when(entityManager.getReference(MotivoMovimiento.class, 1L)).thenReturn(new MotivoMovimiento());
        when(motivoMovimientoRepository.findById(1L)).thenReturn(Optional.of(new MotivoMovimiento()));
        when(loteProductoRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioMapper.toEntity(dto)).thenReturn(new MovimientoInventario());
        when(movimientoInventarioMapper.safeToResponseDTO(any())).thenReturn(new com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO());
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarMovimiento(dto);

        assertEquals(new BigDecimal("7"), lote.getStockLote());
    }

    @Test
    void clasificacionSalidaProduccionConTipoInvalidoLanza422() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass"));

        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder().id(1)
                .categoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto()).build();
        LoteProducto lote = LoteProducto.builder()
                .id(100L)
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .build();
        Usuario usuario = new Usuario(); usuario.setId(1L);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("3"), TipoMovimiento.TRANSFERENCIA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION, null,
                producto.getId(), lote.getId(), origen.getId(), null,
                null, null, null, 1L, 1L, null,
                usuario.getId(), null, null, null, null);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(entityManager.getReference(Almacen.class, origen.getId())).thenReturn(origen);
        when(entityManager.getReference(Usuario.class, usuario.getId())).thenReturn(usuario);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.registrarMovimiento(dto));
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals(new BigDecimal("10"), lote.getStockLote());
    }

    @Test
    void salidaDesdeReservaReduceReservadoYAgota() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass"));

        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder().id(1)
                .categoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto()).build();
        LoteProducto lote = LoteProducto.builder()
                .id(200L)
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("2"))
                .stockReservado(new BigDecimal("2"))
                .build();
        Usuario usuario = new Usuario(); usuario.setId(1L);
        SolicitudMovimiento sol = SolicitudMovimiento.builder()
                .id(5L)
                .tipoMovimiento(TipoMovimiento.SALIDA)
                .producto(producto)
                .lote(lote)
                .cantidad(new BigDecimal("2"))
                .almacenOrigen(origen)
                .usuarioSolicitante(usuario)
                .estado(EstadoSolicitudMovimiento.RESERVADA)
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("2"), TipoMovimiento.SALIDA,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION, null,
                producto.getId(), lote.getId(), origen.getId(), null,
                null, null, null, 1L, 1L, sol.getId(),
                usuario.getId(), null, null, null, null);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(entityManager.getReference(Almacen.class, origen.getId())).thenReturn(origen);
        when(entityManager.getReference(Usuario.class, usuario.getId())).thenReturn(usuario);
        when(entityManager.getReference(TipoMovimientoDetalle.class, 1L)).thenReturn(new TipoMovimientoDetalle());
        when(entityManager.getReference(MotivoMovimiento.class, 1L)).thenReturn(new MotivoMovimiento());
        when(motivoMovimientoRepository.findById(1L)).thenReturn(Optional.of(new MotivoMovimiento()));
        when(loteProductoRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioMapper.toEntity(dto)).thenReturn(new MovimientoInventario());
        when(movimientoInventarioMapper.safeToResponseDTO(any())).thenReturn(new com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO());
        when(movimientoInventarioRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(solicitudMovimientoRepository.findById(sol.getId())).thenReturn(Optional.of(sol));
        when(solicitudMovimientoRepository.save(any(SolicitudMovimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarMovimiento(dto);

        assertTrue(lote.isAgotado());
        assertEquals(new BigDecimal("0"), lote.getStockLote());
        assertEquals(new BigDecimal("0"), lote.getStockReservado());
        assertEquals(EstadoSolicitudMovimiento.ATENDIDA, sol.getEstado());
    }

    @Test
    void entradaPtSinOrdenProduccionLanza422() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass"));

        when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(11L);

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("1"), TipoMovimiento.ENTRADA,
                null, null,
                1, null, null, null,
                null, null, null, 11L, 1L, null,
                1L, null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registrarMovimiento(dto));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID", ex.getReason());
    }

    @Test
    void entradaConLoteExistenteIncrementaStockSinValidarDisponibilidad() {
        Almacen origen = Almacen.builder().id(1).categoria(TipoCategoria.MATERIA_PRIMA).build();
        Producto producto = Producto.builder().id(1).build();
        LoteProducto loteOrigen = LoteProducto.builder()
                .id(100L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(origen)
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("5"))
                .stockReservado(new BigDecimal("3"))
                .build();

        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                null, new BigDecimal("10"), TipoMovimiento.ENTRADA,
                null, null,
                producto.getId(), loteOrigen.getId(), origen.getId(), null,
                null, null, null, null, null, null, null, null, null, null, null);

        when(entityManager.getReference(Almacen.class, dto.almacenOrigenId())).thenReturn(origen);
        when(loteProductoRepository.findById(dto.loteProductoId())).thenReturn(Optional.of(loteOrigen));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        LoteProducto result = ReflectionTestUtils.invokeMethod(service,
                "procesarMovimientoConLoteExistente", dto, TipoMovimiento.ENTRADA,
                origen, null, producto, new BigDecimal("10"), false, null);

        assertEquals(new BigDecimal("15"), result.getStockLote());
    }
}

