package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteProductoServiceImplTest {

    @Mock
    private LoteProductoRepository loteRepo;
    @Mock
    private ProductoRepository productoRepo;
    @Mock
    private AlmacenRepository almacenRepo;
    @Mock
    private LoteProductoMapper loteProductoMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private LoteProductoRepository loteProductoRepositoryForUpdate;
    @Mock
    private EvaluacionCalidadRepository evaluacionRepository;
    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;

    private LoteProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LoteProductoServiceImpl(
                loteRepo,
                productoRepo,
                almacenRepo,
                loteProductoMapper,
                usuarioService,
                loteProductoRepositoryForUpdate,
                evaluacionRepository,
                stockQueryService,
                movimientoInventarioRepository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                catalogResolver
        );
        ReflectionTestUtils.setField(service, "estadoLiberadoConf", "LIBERADO");
    }

    @Test
    void liberarLotePorCalidadMantieneCuarentenaSinMovimiento() {
        Long loteId = 1L;
        Usuario usuarioActual = Usuario.builder()
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .build();

        Almacen almacenCuarentena = Almacen.builder()
                .id(5)
                .nombre("Cuarentena")
                .ubicacion("Zona 1")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();

        Producto producto = new Producto();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.FISICO_QUIMICO);

        LoteProducto lote = LoteProducto.builder()
                .id(loteId)
                .almacen(almacenCuarentena)
                .producto(producto)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(BigDecimal.TEN)
                .stockReservado(BigDecimal.ZERO)
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .build();

        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(5L);
        when(loteProductoRepositoryForUpdate.findByIdForUpdate(loteId)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(loteId)).thenReturn(List.of(
                EvaluacionCalidad.builder()
                        .tipoEvaluacion(TipoEvaluacion.FISICO_QUIMICO)
                        .resultado(ResultadoEvaluacion.CONFORME)
                        .build()
        ));
        when(loteRepo.save(lote)).thenReturn(lote);
        LoteProductoResponseDTO expectedResponse = new LoteProductoResponseDTO();
        when(loteProductoMapper.toResponseDTO(lote)).thenReturn(expectedResponse);

        LoteProductoResponseDTO actual = service.liberarLotePorCalidad(loteId, usuarioActual);

        assertSame(expectedResponse, actual);
        assertEquals(EstadoLote.LIBERADO, lote.getEstado());
        assertSame(almacenCuarentena, lote.getAlmacen());
        assertEquals(usuarioActual, lote.getUsuarioLiberador());
        assertNotNull(lote.getFechaLiberacion());

        verify(loteRepo).save(lote);
        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    void liberarLotePorCalidadEsIdempotenteEnCuarentena() {
        Long loteId = 2L;
        Usuario usuarioActual = Usuario.builder()
                .rol(RolUsuario.ROL_JEFE_CALIDAD)
                .build();

        Almacen almacenCuarentena = Almacen.builder()
                .id(7)
                .nombre("Cuarentena")
                .ubicacion("Zona 2")
                .categoria(TipoCategoria.PRODUCTO_TERMINADO)
                .tipo(TipoAlmacen.PRINCIPAL)
                .build();

        LocalDateTime fechaLiberacion = LocalDateTime.now().minusHours(1);
        LoteProducto lote = LoteProducto.builder()
                .id(loteId)
                .almacen(almacenCuarentena)
                .estado(EstadoLote.LIBERADO)
                .stockLote(BigDecimal.TEN)
                .stockReservado(BigDecimal.ZERO)
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .fechaLiberacion(fechaLiberacion)
                .usuarioLiberador(usuarioActual)
                .build();

        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(loteProductoRepositoryForUpdate.findByIdForUpdate(loteId)).thenReturn(Optional.of(lote));
        LoteProductoResponseDTO expectedResponse = new LoteProductoResponseDTO();
        when(loteProductoMapper.toResponseDTO(lote)).thenReturn(expectedResponse);

        LoteProductoResponseDTO actual = service.liberarLotePorCalidad(loteId, usuarioActual);

        assertSame(expectedResponse, actual);
        assertEquals(EstadoLote.LIBERADO, lote.getEstado());
        assertSame(fechaLiberacion, lote.getFechaLiberacion());
        assertEquals(usuarioActual, lote.getUsuarioLiberador());

        verify(loteRepo, never()).save(any());
        verify(evaluacionRepository, never()).findByLoteProductoId(any());
        verify(movimientoInventarioRepository, never()).save(any());
    }
}
