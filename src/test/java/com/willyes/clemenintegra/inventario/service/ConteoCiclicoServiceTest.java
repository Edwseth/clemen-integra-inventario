package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ConteoCiclicoMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoConteoCiclico;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Test
    void listarConEstadoInvalidoLanzaExcepcion() {
        assertThatThrownBy(() -> conteoCiclicoService.listar(null, "INVALIDO", PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.SOLICITUD_INVALIDA));
    }

    @Test
    void actualizarConteoReemplazaDetallesYRecalcula() {
        Almacen almacen = new Almacen(1);
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(10L)
                .almacen(almacen)
                .estado(EstadoConteoCiclico.BORRADOR)
                .detalles(new java.util.ArrayList<>(List.of(ConteoCiclicoDetalle.builder().id(99L).build())))
                .build();

        Producto productoUno = new Producto();
        productoUno.setId(3);
        Producto productoDos = new Producto();
        productoDos.setId(4);

        LoteProducto loteUno = LoteProducto.builder()
                .id(101L)
                .producto(productoUno)
                .almacen(almacen)
                .stockLote(new BigDecimal("10.00"))
                .build();
        LoteProducto loteDos = LoteProducto.builder()
                .id(102L)
                .producto(productoDos)
                .almacen(almacen)
                .stockLote(new BigDecimal("2.00"))
                .build();

        ConteoCiclicoDetalleRequestDTO detalleUno = new ConteoCiclicoDetalleRequestDTO();
        detalleUno.setProductoId(3L);
        detalleUno.setLoteProductoId(101L);
        detalleUno.setStockSistema(new BigDecimal("10.00"));
        detalleUno.setConteoFisico(new BigDecimal("12.00"));

        ConteoCiclicoDetalleRequestDTO detalleDos = new ConteoCiclicoDetalleRequestDTO();
        detalleDos.setProductoId(4L);
        detalleDos.setLoteProductoId(102L);
        detalleDos.setConteoFisico(new BigDecimal("5.00"));

        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(10L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(3L)).thenReturn(Optional.of(productoUno));
        when(productoRepository.findById(4L)).thenReturn(Optional.of(productoDos));
        when(loteProductoRepository.findById(101L)).thenReturn(Optional.of(loteUno));
        when(loteProductoRepository.findById(102L)).thenReturn(Optional.of(loteDos));
        when(conteoCiclicoRepository.save(any(ConteoCiclico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.actualizarConteo(10L, List.of(detalleUno, detalleDos));

        assertThat(conteo.getDetalles())
                .hasSize(2)
                .allSatisfy(det -> assertThat(det.getDiferencia()).isNotNull());
        assertThat(conteo.getDetalles())
                .anySatisfy(det -> {
                    assertThat(det.getProducto().getId()).isEqualTo(3);
                    assertThat(det.getDiferencia()).isEqualByComparingTo(new BigDecimal("2.00"));
                })
                .anySatisfy(det -> {
                    assertThat(det.getProducto().getId()).isEqualTo(4);
                    assertThat(det.getDiferencia()).isEqualByComparingTo(new BigDecimal("3.00"));
                });
        assertThat(conteo.getDetalles().stream().noneMatch(det -> Long.valueOf(99L).equals(det.getId()))).isTrue();
        assertThat(respuesta.getDetalles()).hasSize(2);
    }

    @Test
    void actualizarConteoRechazaCuandoEstadoEsCerrado() {
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(11L)
                .estado(EstadoConteoCiclico.CERRADO)
                .build();
        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(11L)).thenReturn(Optional.of(conteo));

        ConteoCiclicoDetalleRequestDTO detalle = new ConteoCiclicoDetalleRequestDTO();
        detalle.setProductoId(1L);
        detalle.setConteoFisico(BigDecimal.ONE);

        assertThatThrownBy(() -> conteoCiclicoService.actualizarConteo(11L, List.of(detalle)))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.CONTEO_ESTADO_INVALIDO));

        verify(conteoCiclicoRepository, never()).save(any());
    }

    @Test
    void actualizarConteoConLoteDeOtroProductoLanzaError() {
        Almacen almacen = new Almacen(5);
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(41L)
                .almacen(almacen)
                .estado(EstadoConteoCiclico.BORRADOR)
                .detalles(new java.util.ArrayList<>())
                .build();

        Producto producto = new Producto();
        producto.setId(8);

        Producto otroProducto = new Producto();
        otroProducto.setId(99);

        LoteProducto lote = LoteProducto.builder()
                .id(301L)
                .producto(otroProducto)
                .almacen(almacen)
                .build();

        ConteoCiclicoDetalleRequestDTO detalle = new ConteoCiclicoDetalleRequestDTO();
        detalle.setProductoId(8L);
        detalle.setLoteProductoId(301L);
        detalle.setConteoFisico(BigDecimal.ONE);

        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(41L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(301L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> conteoCiclicoService.actualizarConteo(41L, List.of(detalle)))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.LOTE_INVALIDO_CONTEO));

        verify(conteoCiclicoRepository, never()).save(any());
    }

    @Test
    void actualizarConteoConLoteDeOtroAlmacenLanzaError() {
        Almacen almacen = new Almacen(6);
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(42L)
                .almacen(almacen)
                .estado(EstadoConteoCiclico.BORRADOR)
                .detalles(new java.util.ArrayList<>())
                .build();

        Producto producto = new Producto();
        producto.setId(10);

        LoteProducto lote = LoteProducto.builder()
                .id(302L)
                .producto(producto)
                .almacen(new Almacen(99))
                .build();

        ConteoCiclicoDetalleRequestDTO detalle = new ConteoCiclicoDetalleRequestDTO();
        detalle.setProductoId(10L);
        detalle.setLoteProductoId(302L);
        detalle.setConteoFisico(BigDecimal.TEN);

        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(42L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(302L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> conteoCiclicoService.actualizarConteo(42L, List.of(detalle)))
                .isInstanceOfSatisfying(CustomBusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.LOTE_INVALIDO_CONTEO));

        verify(conteoCiclicoRepository, never()).save(any());
    }

    @Test
    void actualizarConteoConLoteUsaStockDelLote() {
        Almacen almacen = new Almacen(5);
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(31L)
                .almacen(almacen)
                .estado(EstadoConteoCiclico.BORRADOR)
                .detalles(new java.util.ArrayList<>())
                .build();

        Producto producto = new Producto();
        producto.setId(22);

        LoteProducto lote = LoteProducto.builder()
                .id(77L)
                .producto(producto)
                .almacen(almacen)
                .stockLote(new BigDecimal("15.50"))
                .build();

        ConteoCiclicoDetalleRequestDTO detalleRequest = new ConteoCiclicoDetalleRequestDTO();
        detalleRequest.setProductoId(22L);
        detalleRequest.setLoteProductoId(77L);
        detalleRequest.setStockSistema(new BigDecimal("99.99"));
        detalleRequest.setConteoFisico(new BigDecimal("10.00"));

        when(conteoCiclicoRepository.findByIdWithDetallesForUpdate(31L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(22L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.findById(77L)).thenReturn(Optional.of(lote));
        when(conteoCiclicoRepository.save(any(ConteoCiclico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        conteoCiclicoService.actualizarConteo(31L, List.of(detalleRequest));

        assertThat(conteo.getDetalles()).hasSize(1);
        ConteoCiclicoDetalle guardado = conteo.getDetalles().get(0);
        assertThat(guardado.getStockSistema()).isEqualByComparingTo(new BigDecimal("15.50"));
        assertThat(guardado.getDiferencia()).isEqualByComparingTo(new BigDecimal("-5.50"));
    }

    @Test
    void listarLotesIncluyeEstadosNoOperables() {
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(2L)
                .almacen(new Almacen(7))
                .build();
        Producto producto = new Producto();
        producto.setId(3);

        LoteProducto loteRetenido = LoteProducto.builder()
                .id(15L)
                .producto(producto)
                .almacen(conteo.getAlmacen())
                .codigoLote("RET-001")
                .estado(EstadoLote.RETENIDO)
                .stockLote(BigDecimal.ZERO)
                .build();

        when(conteoCiclicoRepository.findById(2L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(3L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.buscarParaConteo(eq(3L), eq(7), isNull(), isNull(), anyCollection()))
                .thenReturn(List.of(loteRetenido));

        ArgumentCaptor<Collection<EstadoLote>> estadosCaptor = ArgumentCaptor.forClass(Collection.class);

        List<com.willyes.clemenintegra.inventario.dto.ConteoCiclicoLoteResponseDTO> respuesta = conteoCiclicoService
                .listarLotesParaConteo(2L, 3L, null, null);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().getId()).isEqualTo(15L);

        verify(loteProductoRepository).buscarParaConteo(eq(3L), eq(7), isNull(), isNull(), estadosCaptor.capture());
        assertThat(estadosCaptor.getValue()).containsExactlyInAnyOrder(EstadoLote.values());
    }

    @Test
    void listarLotesParaConteoSinBuscarTextoNoFiltraPorCodigo() {
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(12L)
                .almacen(new Almacen(5))
                .build();
        Producto producto = new Producto();
        producto.setId(8);

        LoteProducto lote = LoteProducto.builder()
                .id(99L)
                .producto(producto)
                .almacen(conteo.getAlmacen())
                .codigoLote("FAFAFSAF")
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.ONE)
                .build();

        when(conteoCiclicoRepository.findById(12L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.buscarParaConteo(eq(8L), eq(5), isNull(), isNull(), anyCollection()))
                .thenReturn(List.of(lote));

        List<ConteoCiclicoLoteResponseDTO> respuesta = conteoCiclicoService
                .listarLotesParaConteo(12L, 8L, null, null);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().getCodigoLote()).isEqualTo("FAFAFSAF");
    }

    @Test
    void listarLotesParaConteoConTextoAplicaFiltro() {
        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(13L)
                .almacen(new Almacen(6))
                .build();
        Producto producto = new Producto();
        producto.setId(9);

        LoteProducto lote = LoteProducto.builder()
                .id(101L)
                .producto(producto)
                .almacen(conteo.getAlmacen())
                .codigoLote("L20251003-01")
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(BigDecimal.TEN)
                .build();

        when(conteoCiclicoRepository.findById(13L)).thenReturn(Optional.of(conteo));
        when(productoRepository.findById(9L)).thenReturn(Optional.of(producto));
        when(loteProductoRepository.buscarParaConteo(eq(9L), eq(6), isNull(), anyString(), anyCollection()))
                .thenReturn(List.of(lote));

        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);

        List<ConteoCiclicoLoteResponseDTO> respuesta = conteoCiclicoService
                .listarLotesParaConteo(13L, 9L, null, " 20251003 ");

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().getCodigoLote()).isEqualTo("L20251003-01");

        verify(loteProductoRepository).buscarParaConteo(eq(9L), eq(6), isNull(), qCaptor.capture(), anyCollection());
        assertThat(qCaptor.getValue()).isEqualTo("20251003");
    }

    @Test
    void mapperIncluyeCodigoDeLoteEnRespuesta() {
        ConteoCiclicoMapper mapper = new ConteoCiclicoMapper();
        Almacen almacen = new Almacen(10);
        Producto producto = new Producto();
        producto.setId(15);
        LoteProducto lote = LoteProducto.builder()
                .id(401L)
                .producto(producto)
                .almacen(almacen)
                .codigoLote("L-0401")
                .build();

        ConteoCiclicoDetalle detalle = ConteoCiclicoDetalle.builder()
                .id(501L)
                .producto(producto)
                .loteProducto(lote)
                .stockSistema(BigDecimal.ZERO)
                .conteoFisico(BigDecimal.ZERO)
                .diferencia(BigDecimal.ZERO)
                .build();

        ConteoCiclico conteo = ConteoCiclico.builder()
                .id(70L)
                .almacen(almacen)
                .detalles(new java.util.ArrayList<>(List.of(detalle)))
                .build();
        detalle.setConteo(conteo);

        ConteoCiclicoResponseDTO response = mapper.toResponse(conteo);

        assertThat(response.getDetalles()).hasSize(1);
        assertThat(response.getDetalles().getFirst().getLoteCodigo()).isEqualTo("L-0401");
    }
}
