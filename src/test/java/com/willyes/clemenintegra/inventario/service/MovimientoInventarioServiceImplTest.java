package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
                dto, TipoMovimiento.TRANSFERENCIA, origen, destino, producto, new BigDecimal("5"), false);

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
                dto, TipoMovimiento.TRANSFERENCIA, origen, destino, producto, new BigDecimal("10"), false);

        assertSame(loteOrigen, result);
        assertEquals(EstadoLote.RECHAZADO, result.getEstado());
        assertEquals(destino, result.getAlmacen());
    }
}

