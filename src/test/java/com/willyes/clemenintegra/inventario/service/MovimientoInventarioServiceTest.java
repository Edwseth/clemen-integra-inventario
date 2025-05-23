package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovimientoInventarioServiceTest {

    @Mock private MovimientoInventarioRepository repository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @InjectMocks
    private MovimientoInventarioServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registrarMovimiento_DeberiaGuardarMovimientoCorrectamente() {
        // Arrange
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10),
                ClasificacionMovimientoInventario.ENTRADA_PRODUCCION,
                "DOC-REF-001",
                1L,1L,1L,1L,1L,1L,1L, 1L
        );

        // Entidades
        Producto producto = new Producto(); producto.setId(1L);
        Almacen almacen = new Almacen(); almacen.setId(1L);
        Proveedor proveedor = new Proveedor(); proveedor.setId(1L);
        OrdenCompra ordenCompra = new OrdenCompra(); ordenCompra.setId(1L);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(1L);
        LoteProducto lote = new LoteProducto(); lote.setId(1L);
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(1L);
        detalle.setDescripcion(dto.tipoMovimiento().name());

        // Stubs de repos
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacen));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(ordenCompra));
        when(motivoMovimientoRepository.findById(1L)).thenReturn(Optional.of(motivo));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(tipoMovimientoDetalleRepository.findById(1L)).thenReturn(Optional.of(detalle));

        // Aquí: devolver el mismo mv con id seteado
        when(repository.save(any())).thenAnswer(invocation -> {
            MovimientoInventario mv = invocation.getArgument(0);
            mv.setId(42L);
            return mv;
        });

        // Act
        MovimientoInventarioDTO result = service.registrarMovimiento(dto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(10), result.cantidad());
        assertEquals(1L, result.loteProductoId());
        assertEquals(1L, result.tipoMovimientoDetalleId());
        verify(repository).save(any(MovimientoInventario.class));
    }


    @Test
    void registrarMovimiento_DeberiaLanzarExceptionCuandoProductoNoExiste() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.ONE,                                      // 1) cantidad
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,// 2) tipoMovimiento
                "DOC-REF-002",                                       // 3) docReferencia
                999L,1L,1L,1L,1L,1L,1L, 1L
        );

        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.registrarMovimiento(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarMovimiento_DeberiaLanzarExcepcionPorInconsistenciaTipoYDetalle() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.ONE,                                     // cantidad
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION, // tipoMovimiento
                "DOC-REF-003",                                       // docReferencia
                1L,1L,1L,1L,1L,1L,1L,1L
        );

        // Stub repos antes de validación
        when(productoRepository.findById(1L)).thenReturn(Optional.of(new Producto()));
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen()));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(new Proveedor()));
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(new OrdenCompra()));
        when(motivoMovimientoRepository.findById(1L)).thenReturn(Optional.of(new MotivoMovimiento()));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(new LoteProducto()));
        // Detalle con descripción distinta a dto.tipoMovimiento().name()
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(1L);
        detalle.setDescripcion("OTRA_CLASIFICACION");
        when(tipoMovimientoDetalleRepository.findById(1L))
                .thenReturn(Optional.of(detalle));

        // Assert: ahora lanza IllegalArgumentException, no NPE
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarMovimiento(dto));
        verify(repository, never()).save(any());
    }

}


