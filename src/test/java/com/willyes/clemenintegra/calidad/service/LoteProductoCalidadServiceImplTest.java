package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.LoteProductoServiceImpl;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteProductoCalidadServiceImplTest {

    @Mock LoteProductoRepository loteRepo;
    @Mock LoteProductoRepository loteProductoRepository;
    @Mock ProductoRepository productoRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock LoteProductoMapper loteProductoMapper;
    @Mock UsuarioService usuarioService;
    @Mock EvaluacionCalidadRepository evaluacionRepository;
    @Mock InventoryCatalogResolver catalogResolver;
    @Mock MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock StockQueryService stockQueryService;

    LoteProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LoteProductoServiceImpl(
                loteRepo,
                productoRepository,
                almacenRepository,
                loteProductoMapper,
                usuarioService,
                loteProductoRepository,
                evaluacionRepository,
                stockQueryService,
                movimientoInventarioRepository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                catalogResolver
        );
        ReflectionTestUtils.setField(service, "estadoLiberadoConf", "DISPONIBLE");
        ReflectionTestUtils.setField(service, "clasificacionLiberacionConf", "LIBERACION_CALIDAD");
        ReflectionTestUtils.setField(service, "clasificacionRechazoCalidad", "RECHAZO_CALIDAD");
        when(loteProductoMapper.toResponseDTO(any())).thenAnswer(inv -> {
            LoteProducto l = inv.getArgument(0);
            LoteProductoResponseDTO dto = new LoteProductoResponseDTO();
            dto.setId(l.getId());
            dto.setEstado(l.getEstado());
            dto.setAlmacenId(l.getAlmacen().getId());
            return dto;
        });
    }

    private LoteProducto buildLote(EstadoLote estado, Almacen almacen, Producto producto) {
        return LoteProducto.builder()
                .id(1L)
                .codigoLote("L1")
                .producto(producto)
                .almacen(almacen)
                .estado(estado)
                .stockLote(BigDecimal.ONE)
                .build();
    }

    @Test
    void liberacionCambiaEstadoYCreaTransferencia() {
        Usuario jefe = new Usuario(); jefe.setRol(RolUsuario.ROL_JEFE_CALIDAD); jefe.setId(10L);
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        Almacen cuarentena = Almacen.builder().id(7L).build();
        Almacen pt = Almacen.builder().id(2L).build();
        LoteProducto lote = buildLote(EstadoLote.EN_CUARENTENA, cuarentena, producto);

        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(1L)).thenReturn(List.of(
                EvaluacionCalidad.builder().tipoEvaluacion(TipoEvaluacion.FISICO_QUIMICO).resultado(ResultadoEvaluacion.CONFORME).build(),
                EvaluacionCalidad.builder().tipoEvaluacion(TipoEvaluacion.MICROBIOLOGICO).resultado(ResultadoEvaluacion.CONFORME).build()
        ));
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(12L);
        when(motivoMovimientoRepository.findById(12L)).thenReturn(java.util.Optional.of(new MotivoMovimiento()));
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(java.util.Optional.of(new TipoMovimientoDetalle()));
        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(2L)).thenReturn(java.util.Optional.of(pt));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(any(), anyLong(), anyLong(), anyLong(), any())).thenReturn(false);
        when(movimientoInventarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoteProductoResponseDTO result = service.liberarLotePorCalidad(1L, jefe);

        assertEquals(EstadoLote.DISPONIBLE, lote.getEstado());
        assertEquals(pt.getId(), lote.getAlmacen().getId());
        assertEquals(EstadoLote.DISPONIBLE, result.getEstado());
        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertEquals(TipoMovimiento.TRANSFERENCIA, movCaptor.getValue().getTipoMovimiento());
    }

    @Test
    void liberacionIdempotenteNoDuplicaMovimiento() {
        Usuario jefe = new Usuario(); jefe.setRol(RolUsuario.ROL_JEFE_CALIDAD); jefe.setId(10L);
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        Almacen cuarentena = Almacen.builder().id(7L).build();
        Almacen pt = Almacen.builder().id(2L).build();
        LoteProducto lote = buildLote(EstadoLote.EN_CUARENTENA, cuarentena, producto);

        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(1L)).thenReturn(List.of(
                EvaluacionCalidad.builder().tipoEvaluacion(TipoEvaluacion.FISICO_QUIMICO).resultado(ResultadoEvaluacion.CONFORME).build(),
                EvaluacionCalidad.builder().tipoEvaluacion(TipoEvaluacion.MICROBIOLOGICO).resultado(ResultadoEvaluacion.CONFORME).build()
        ));
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(12L);
        when(motivoMovimientoRepository.findById(12L)).thenReturn(java.util.Optional.of(new MotivoMovimiento()));
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(java.util.Optional.of(new TipoMovimientoDetalle()));
        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(2L)).thenReturn(java.util.Optional.of(pt));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(any(), anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(false).thenReturn(true);
        when(movimientoInventarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.liberarLotePorCalidad(1L, jefe);
        service.liberarLotePorCalidad(1L, jefe);

        verify(movimientoInventarioRepository, times(1)).save(any());
    }

    @Test
    void liberacionFallaSiFaltanEvaluacionesRequeridas() {
        Usuario jefe = new Usuario(); jefe.setRol(RolUsuario.ROL_JEFE_CALIDAD); jefe.setId(10L);
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        Almacen cuarentena = Almacen.builder().id(7L).build();
        LoteProducto lote = buildLote(EstadoLote.EN_CUARENTENA, cuarentena, producto);

        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(1L)).thenReturn(List.of(
                EvaluacionCalidad.builder().tipoEvaluacion(TipoEvaluacion.FISICO_QUIMICO).resultado(ResultadoEvaluacion.CONFORME).build()
        ));
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(12L);
        when(motivoMovimientoRepository.findById(12L)).thenReturn(java.util.Optional.of(new MotivoMovimiento()));
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(java.util.Optional.of(new TipoMovimientoDetalle()));
        when(catalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(2L)).thenReturn(java.util.Optional.of(Almacen.builder().id(2L).build()));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(any(), anyLong(), anyLong(), anyLong(), any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.liberarLotePorCalidad(1L, jefe));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Falta evaluación requerida", ex.getReason());
        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    void rechazoMueveAObsoletosConMotivoAjuste() {
        Usuario jefe = new Usuario(); jefe.setId(20L); jefe.setRol(RolUsuario.ROL_JEFE_CALIDAD);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(jefe);
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        Almacen cuarentena = Almacen.builder().id(7L).build();
        Almacen obsoletos = Almacen.builder().id(3L).build();
        LoteProducto lote = buildLote(EstadoLote.EN_CUARENTENA, cuarentena, producto);

        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(1L)).thenReturn(List.of(new EvaluacionCalidad()));
        when(catalogResolver.getMotivoIdAjusteRechazo()).thenReturn(30L);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(30L);
        when(motivoMovimientoRepository.findById(30L)).thenReturn(java.util.Optional.of(motivo));
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(java.util.Optional.of(new TipoMovimientoDetalle()));
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(3L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(3L)).thenReturn(java.util.Optional.of(obsoletos));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(any(), anyLong(), anyLong(), anyLong(), any())).thenReturn(false);
        when(movimientoInventarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoteProductoResponseDTO result = service.rechazarLote(1L);

        assertEquals(EstadoLote.RECHAZADO, lote.getEstado());
        assertEquals(obsoletos.getId(), lote.getAlmacen().getId());
        ArgumentCaptor<MovimientoInventario> movCaptor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertEquals(motivo.getId(), movCaptor.getValue().getMotivoMovimiento().getId());
        assertEquals(TipoMovimiento.TRANSFERENCIA, movCaptor.getValue().getTipoMovimiento());
        assertEquals(EstadoLote.RECHAZADO, result.getEstado());
    }

    @Test
    void rechazoIdempotente() {
        Usuario jefe = new Usuario(); jefe.setId(20L); jefe.setRol(RolUsuario.ROL_JEFE_CALIDAD);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(jefe);
        Producto producto = Producto.builder().id(1).tipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS).build();
        Almacen cuarentena = Almacen.builder().id(7L).build();
        Almacen obsoletos = Almacen.builder().id(3L).build();
        LoteProducto lote = buildLote(EstadoLote.EN_CUARENTENA, cuarentena, producto);

        when(loteProductoRepository.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(1L)).thenReturn(List.of(new EvaluacionCalidad()));
        when(catalogResolver.getMotivoIdAjusteRechazo()).thenReturn(30L);
        MotivoMovimiento motivo = new MotivoMovimiento(); motivo.setId(30L);
        when(motivoMovimientoRepository.findById(30L)).thenReturn(java.util.Optional.of(motivo));
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(9L);
        when(tipoMovimientoDetalleRepository.findById(9L)).thenReturn(java.util.Optional.of(new TipoMovimientoDetalle()));
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(3L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(almacenRepository.findById(3L)).thenReturn(java.util.Optional.of(obsoletos));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(any(), anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(false).thenReturn(true);
        when(movimientoInventarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rechazarLote(1L);
        service.rechazarLote(1L);

        verify(movimientoInventarioRepository, times(1)).save(any());
        assertEquals(EstadoLote.RECHAZADO, lote.getEstado());
        assertEquals(obsoletos.getId(), lote.getAlmacen().getId());
    }
}

