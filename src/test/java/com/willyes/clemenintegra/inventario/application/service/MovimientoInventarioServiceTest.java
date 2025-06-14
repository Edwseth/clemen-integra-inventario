package com.willyes.clemenintegra.inventario.application.service;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.domain.enums.*;
import com.willyes.clemenintegra.inventario.domain.model.*;
import com.willyes.clemenintegra.inventario.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mapstruct.factory.Mappers;
import com.willyes.clemenintegra.inventario.application.mapper.MovimientoInventarioMapper;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovimientoInventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository repository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;

    private MovimientoInventarioService service;

    private final MovimientoInventarioMapper mapper =
            Mappers.getMapper(MovimientoInventarioMapper.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MovimientoInventarioService(
                repository,
                productoRepository,
                almacenRepository,
                proveedorRepository,
                ordenCompraRepository,
                motivoMovimientoRepository,
                mapper
        );
    }

    @Test
    void registrarMovimiento_DeberiaGuardarMovimientoCorrectamente() {
        // Arrange: crear el DTO de entrada
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(10),
                TipoMovimiento.ENTRADA_PRODUCCION,
                "DOC-REF-001",
                1L, // productoId
                1L, // almacenId
                1L, // proveedorId
                1L, // ordenCompraId
                1L, // motivoMovimientoId
                1L, // usuarioRegistroId
                TipoMovimientoDetalle.ENTRADA_PRODUCCION,
                1L  // loteId
        );

        // Mock de entidades relacionadas
        Producto producto = new Producto(); producto.setId(1L);
        Almacen almacen = new Almacen(); almacen.setId(1L);
        Proveedor proveedor = new Proveedor(); proveedor.setId(1L);
        OrdenCompra ordenCompra = new OrdenCompra(); ordenCompra.setId(1L);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(1L);
        LoteProducto lote = new LoteProducto(); lote.setId(1L); lote.setCodigoLote("LT-123");

        // Stub de repositorios
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacen));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(ordenCompra));
        when(motivoMovimientoRepository.findById(1L)).thenReturn(Optional.of(motivo));
        when(loteProductoRepository.findById(1L)).thenReturn(Optional.of(lote));

        // Mock del usuario que registra el movimiento
        Usuario usuario = new Usuario(); usuario.setId(1L);

        // Mock del movimiento para simular el guardado en base de datos
        MovimientoInventario movimientoMock = new MovimientoInventario();
        movimientoMock.setId(1L);
        movimientoMock.setProducto(producto);
        movimientoMock.setAlmacen(almacen);
        movimientoMock.setProveedor(proveedor);
        movimientoMock.setOrdenCompra(ordenCompra);
        movimientoMock.setMotivoMovimiento(motivo);
        movimientoMock.setLote(lote);
        movimientoMock.setCantidad(BigDecimal.valueOf(10));
        movimientoMock.setTipoMovimiento(TipoMovimiento.ENTRADA_PRODUCCION);
        movimientoMock.setTipoMovimientoDetalle(TipoMovimientoDetalle.ENTRADA_PRODUCCION);
        movimientoMock.setRegistradoPor(usuario);

        when(repository.save(any(MovimientoInventario.class))).thenReturn(movimientoMock);

        // Act
        MovimientoInventarioDTO resultado = service.registrarMovimiento(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(dto.productoId(), resultado.productoId());
        assertEquals(dto.loteId(), resultado.loteId());
        verify(repository).save(any(MovimientoInventario.class));
    }


    @Test
    void registrarMovimiento_DeberiaLanzarExcepcionCuandoProductoNoExiste() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(5),
                TipoMovimiento.SALIDA_PRODUCCION,
                "DOC-REF-002",
                Long.valueOf(999),
                Long.valueOf(1),
                Long.valueOf(1),
                Long.valueOf(1),
                (Long) null,
                (Long) null,
                TipoMovimientoDetalle.SALIDA_PRODUCCION,
                Long.valueOf(1)
        );

        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.registrarMovimiento(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarMovimiento_DeberiaLanzarExcepcionPorInconsistenciaEntreTipoYDetalle() {
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO(
                BigDecimal.valueOf(5),
                TipoMovimiento.SALIDA_PRODUCCION,
                "DOC-REF-003",
                Long.valueOf(1),
                Long.valueOf(1),
                Long.valueOf(1),
                (Long) null,
                (Long) null,
                (Long) null,
                TipoMovimientoDetalle.ENTRADA_PRODUCCION,
                Long.valueOf(1)
        );

        Producto producto = new Producto(); producto.setId(1L);
        Almacen almacen = new Almacen(); almacen.setId(1L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacen));

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimiento(dto));
        verify(repository, never()).save(any());
    }
}

