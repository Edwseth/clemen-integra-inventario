package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ConteoCiclicoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConteoCiclicoServiceTest {

    @Mock
    private ConteoCiclicoRepository conteoCiclicoRepository;
    @Mock
    private ConteoCiclicoDetalleRepository detalleRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private UbicacionFisicaRepository ubicacionFisicaRepository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private ConteoCiclicoService conteoCiclicoService;

    @Captor
    private ArgumentCaptor<com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO> movimientoCaptor;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        conteoCiclicoService = new ConteoCiclicoService(
                conteoCiclicoRepository,
                detalleRepository,
                almacenRepository,
                productoRepository,
                loteProductoRepository,
                ubicacionFisicaRepository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                movimientoInventarioService,
                usuarioService,
                new ConteoCiclicoMapper()
        );
        usuario = Usuario.builder().id(9L).nombreUsuario("tester").build();
        lenient().when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
    }

    @Test
    void aplicarGeneraAjustesPositivoYNegativo() {
        Almacen almacen = new Almacen(1);
        Producto producto = new Producto();
        producto.setId(5);

        LoteProducto lotePositivo = LoteProducto.builder()
                .id(100L)
                .producto(producto)
                .almacen(almacen)
                .codigoLote("L-100")
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("5"))
                .stockReservado(BigDecimal.ZERO)
                .build();

        LoteProducto loteNegativo = LoteProducto.builder()
                .id(200L)
                .producto(producto)
                .almacen(almacen)
                .codigoLote("L-200")
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10"))
                .stockReservado(BigDecimal.ZERO)
                .build();

        ConteoCiclicoDetalle detallePositivo = ConteoCiclicoDetalle.builder()
                .id(1L)
                .producto(producto)
                .loteProducto(lotePositivo)
                .stockSistema(new BigDecimal("5.00"))
                .conteoFisico(new BigDecimal("8.00"))
                .diferencia(BigDecimal.ZERO)
                .build();
        ConteoCiclicoDetalle detalleNegativo = ConteoCiclicoDetalle.builder()
                .id(2L)
                .producto(producto)
                .loteProducto(loteNegativo)
                .stockSistema(new BigDecimal("10.00"))
                .conteoFisico(new BigDecimal("6.00"))
                .diferencia(BigDecimal.ZERO)
                .build();

        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(50L)
                .almacen(almacen)
                .estado(EstadoConteoCiclico.CERRADO)
                .detalles(new java.util.ArrayList<>(List.of(detallePositivo, detalleNegativo)))
                .build();
        detallePositivo.setConteo(conteo);
        detalleNegativo.setConteo(conteo);

        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(50L)).thenReturn(Optional.of(conteo));
        when(loteProductoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lotePositivo));
        when(loteProductoRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(loteNegativo));
        when(motivoMovimientoRepository.findByMotivo(any(ClasificacionMovimientoInventario.class)))
                .thenAnswer(invocation -> Optional.of(MotivoMovimiento.builder()
                        .id(invocation.getArgument(0) == ClasificacionMovimientoInventario.AJUSTE_POSITIVO ? 2L : 1L)
                        .motivo(invocation.getArgument(0))
                        .descripcion("motivo")
                        .build()));
        when(tipoMovimientoDetalleRepository.findByDescripcion(anyString()))
                .thenAnswer(invocation -> Optional.of(TipoMovimientoDetalle.builder()
                        .id(invocation.getArgument(0).toString().contains("POSITIVO") ? 2L : 1L)
                        .descripcion(invocation.getArgument(0).toString())
                        .build()));
        when(conteoCiclicoRepository.save(any(ConteoCiclico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(detalleRepository.save(any(ConteoCiclicoDetalle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.aplicar(50L, "key-1");

        verify(movimientoInventarioService, times(2)).registrarMovimiento(movimientoCaptor.capture(), any());
        List<com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO> movimientos = movimientoCaptor.getAllValues();
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos)
                .anySatisfy(mov -> {
                    assertThat(mov.tipoMovimiento()).isEqualTo(TipoMovimiento.AJUSTE);
                    assertThat(mov.clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.AJUSTE_POSITIVO);
                    assertThat(mov.cantidad()).isEqualByComparingTo(new BigDecimal("3.00"));
                })
                .anySatisfy(mov -> {
                    assertThat(mov.tipoMovimiento()).isEqualTo(TipoMovimiento.AJUSTE);
                    assertThat(mov.clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.AJUSTE_NEGATIVO);
                    assertThat(mov.cantidad()).isEqualByComparingTo(new BigDecimal("4.00"));
                });

        assertThat(conteo.getEstado()).isEqualTo(EstadoConteoCiclico.APLICADO);
        assertThat(conteo.getAplicadoPor()).isEqualTo(usuario);
        assertThat(respuesta.getAplicadoPorId()).isEqualTo(usuario.getId());
        assertThat(detallePositivo.getDiferencia()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(detalleNegativo.getDiferencia()).isEqualByComparingTo(new BigDecimal("-4.00"));
    }

    @Test
    void aplicarIdempotenteRechazaSegundaVez() {
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(99L)
                .estado(EstadoConteoCiclico.APLICADO)
                .aplicadoEn(LocalDateTime.now())
                .build();
        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(99L)).thenReturn(Optional.of(conteo));

        assertThatThrownBy(() -> conteoCiclicoService.aplicar(99L, null))
                .isInstanceOf(CustomBusinessException.class);
    }
}
