package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteProductoCalidadServiceTest {

    @Mock
    private LoteProductoRepository loteRepo;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private LoteProductoMapper loteProductoMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private LoteProductoRepository loteProductoRepository;
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
        service = new LoteProductoServiceImpl(loteRepo, productoRepository, almacenRepository, loteProductoMapper,
                usuarioService, loteProductoRepository, evaluacionRepository, stockQueryService,
                movimientoInventarioRepository, motivoMovimientoRepository, tipoMovimientoDetalleRepository,
                catalogResolver);
        ReflectionTestUtils.setField(service, "estadoLiberadoConf", "LIBERADO");
        ReflectionTestUtils.setField(service, "clasificacionLiberacionConf", "LIBERACION_CALIDAD");
        ReflectionTestUtils.setField(service, "clasificacionRechazoCalidad", "RECHAZO_CALIDAD");
    }

    @Test
    void liberarLotePorCalidadMueveLoteAPrincipal() {
        Usuario jefe = buildUsuario(RolUsuario.ROL_JEFE_CALIDAD);

        Producto producto = new Producto();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS);
        LoteProducto lote = buildLote(42L, 7, producto);

        when(loteProductoRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(lote));
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(55L);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(66L);
        when(motivoMovimientoRepository.findById(55L)).thenReturn(Optional.of(MotivoMovimiento.builder()
                .id(55L)
                .motivo(ClasificacionMovimientoInventario.LIBERACION_CALIDAD)
                .build()));
        when(tipoMovimientoDetalleRepository.findById(66L)).thenReturn(Optional.of(TipoMovimientoDetalle.builder()
                .id(66L)
                .descripcion("Transferencia")
                .build()));
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                eq(TipoMovimiento.TRANSFERENCIA), eq(42L), eq(7L), eq(1L), eq(ClasificacionMovimientoInventario.LIBERACION_CALIDAD)))
                .thenReturn(false);
        when(evaluacionRepository.findByLoteProductoId(42L)).thenReturn(List.of(
                buildEvaluacion(TipoEvaluacion.MICROBIOLOGICO),
                buildEvaluacion(TipoEvaluacion.FISICO_QUIMICO)));
        when(loteProductoMapper.toResponseDTO(any())).thenAnswer(invocation -> {
            LoteProducto value = invocation.getArgument(0);
            return LoteProductoResponseDTO.builder()
                    .id(value.getId())
                    .estado(value.getEstado())
                    .build();
        });
        when(loteRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoInventarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoteProductoResponseDTO respuesta = service.liberarLotePorCalidad(42L, jefe);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(1);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        verify(movimientoInventarioRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void liberarLotePorCalidadFallaSiAlmacenNoEsCuarentena() {
        Usuario jefe = buildUsuario(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = new Producto();
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.MICROBIOLOGICO);
        LoteProducto lote = buildLote(77L, 1, producto);

        when(loteProductoRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(lote));
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);

        assertThatThrownBy(() -> service.liberarLotePorCalidad(77L, jefe))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private Usuario buildUsuario(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setId(10L);
        return usuario;
    }

    private LoteProducto buildLote(Long id, Integer almacenId, Producto producto) {
        LoteProducto lote = new LoteProducto();
        lote.setId(id);
        lote.setAlmacen(new Almacen(almacenId));
        lote.setEstado(EstadoLote.EN_CUARENTENA);
        lote.setStockLote(BigDecimal.TEN);
        lote.setStockReservado(BigDecimal.ZERO);
        lote.setProducto(producto);
        lote.setFechaLiberacion(null);
        lote.setUsuarioLiberador(null);
        lote.setFechaVencimiento(LocalDateTime.now().plusDays(5));
        return lote;
    }

    private EvaluacionCalidad buildEvaluacion(TipoEvaluacion tipo) {
        return EvaluacionCalidad.builder()
                .tipoEvaluacion(tipo)
                .resultado(ResultadoEvaluacion.CONFORME)
                .build();
    }
}
