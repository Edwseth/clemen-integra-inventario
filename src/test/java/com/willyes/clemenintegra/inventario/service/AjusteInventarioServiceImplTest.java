package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.TipoAjuste;
import com.willyes.clemenintegra.inventario.mapper.AjusteInventarioMapper;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.AjusteInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AjusteInventarioServiceImplTest {

    @Mock
    private AjusteInventarioRepository repository;
    @Mock
    private AjusteInventarioMapper mapper;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;

    @InjectMocks
    private AjusteInventarioServiceImpl service;

    private Producto producto;
    private Almacen almacen;
    private LoteProducto lote;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(10);

        almacen = new Almacen();
        almacen.setId(2);

        lote = new LoteProducto();
        lote.setId(99L);
        lote.setProducto(producto);
        lote.setAlmacen(almacen);
        lote.setCodigoLote("L-99");

        usuario = new Usuario();
        usuario.setId(5L);

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(100L);

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(200L);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(almacen));
        when(loteProductoRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(lote));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO))
                .thenReturn(Optional.of(motivo));
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.AJUSTE_POSITIVO))
                .thenReturn(Optional.of(motivo));
        when(tipoMovimientoDetalleRepository.findByDescripcion(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO.name()))
                .thenReturn(Optional.of(tipoDetalle));
        when(tipoMovimientoDetalleRepository.findByDescripcion(ClasificacionMovimientoInventario.AJUSTE_POSITIVO.name()))
                .thenReturn(Optional.of(tipoDetalle));

        AjusteInventario ajuste = new AjusteInventario();
        when(mapper.toEntity(any(), any(), any(), any())).thenReturn(ajuste);
        when(repository.save(any(AjusteInventario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponseDTO(any(AjusteInventario.class)))
                .thenReturn(com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO.builder().id(1L).build());
    }

    @Test
    void ajuste_negativo_crea_movimiento_y_decrementa_stock_lote() {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("5"))
                .tipoAjuste(TipoAjuste.NEGATIVO)
                .motivo("Ajuste negativo")
                .observaciones("descuento")
                .productoId(10L)
                .almacenId(2L)
                .loteProductoId(99L)
                .build();

        service.crear(dto);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture());
        MovimientoInventarioDTO movimiento = captor.getValue();
        assertThat(movimiento.clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO);
        assertThat(movimiento.cantidad()).isEqualByComparingTo(new BigDecimal("5"));
        verify(repository).save(any(AjusteInventario.class));
    }

    @Test
    void ajuste_negativo_no_permite_stock_negativo() {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("5000"))
                .tipoAjuste(TipoAjuste.NEGATIVO)
                .motivo("Ajuste negativo")
                .observaciones("insuficiente")
                .productoId(10L)
                .almacenId(2L)
                .loteProductoId(99L)
                .build();

        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_STOCK_INSUFICIENTE"))
                .when(movimientoInventarioService).registrarMovimiento(any(MovimientoInventarioDTO.class));

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422 UNPROCESSABLE_ENTITY");

        verify(repository, never()).save(any(AjusteInventario.class));
    }

    @Test
    void ajuste_positivo_crea_movimiento_y_incrementa_stock_lote() {
        AjusteInventarioRequestDTO dto = AjusteInventarioRequestDTO.builder()
                .cantidad(new BigDecimal("7"))
                .tipoAjuste(TipoAjuste.POSITIVO)
                .motivo("Ajuste positivo")
                .observaciones("sumar")
                .productoId(10L)
                .almacenId(2L)
                .loteProductoId(99L)
                .build();

        service.crear(dto);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture());
        MovimientoInventarioDTO movimiento = captor.getValue();
        assertThat(movimiento.clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.AJUSTE_POSITIVO);
        assertThat(movimiento.cantidad()).isEqualByComparingTo(new BigDecimal("7"));
        verify(repository).save(any(AjusteInventario.class));
    }
}
