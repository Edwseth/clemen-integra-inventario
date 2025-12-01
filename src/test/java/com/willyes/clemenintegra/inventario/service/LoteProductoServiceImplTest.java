package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.mapper.CondicionUsoMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.CondicionUsoRepository;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoteProductoServiceImplTest {

    @Mock private ProductoRepository productoRepo;
    @Mock private AlmacenRepository almacenRepo;
    @Mock private LoteProductoMapper loteProductoMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private EvaluacionCalidadRepository evaluacionRepository;
    @Mock private StockQueryService stockQueryService;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private RetencionLoteService retencionLoteService;
    @Mock private NoConformidadService noConformidadService;
    @Mock private CondicionUsoService condicionUsoService;
    @Mock private CondicionUsoRepository condicionUsoRepository;
    @Mock private CondicionUsoMapper condicionUsoMapper;
    @Mock private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;

    @InjectMocks
    private LoteProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "estadoLiberadoConf", "LIBERADO");
        ReflectionTestUtils.setField(service, "clasificacionLiberacionConf", "LIBERACION_CALIDAD");
        ReflectionTestUtils.setField(service, "clasificacionRechazoCalidad", "RECHAZO_CALIDAD");

        when(retencionLoteService.obtenerRetencionesActivas(anyLong())).thenReturn(Collections.emptyList());
        when(noConformidadService.obtenerActivaPorLote(anyLong())).thenReturn(Optional.empty());
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                any(), anyLong(), anyLong(), anyLong(), any())).thenReturn(false);
    }

    @Test
    @DisplayName("Debe liberar lote de PT usando su almacén principal")
    void liberarLoteProductoTerminado() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        LoteProducto lote = loteEnCuarentena(10L, producto, 7, new BigDecimal("5.50"));

        mockCatalogosBasicos(13L, 12L);
        when(catalogResolver.resolveAlmacenPrincipal(producto)).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(loteProductoRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(10L)).thenReturn(evaluacionesConforme());
        when(almacenRepo.findById(2L)).thenReturn(Optional.of(almacenConId(2)));

        service.liberarLotePorCalidad(10L, jefeCalidad);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(2L);

        ArgumentCaptor<com.willyes.clemenintegra.inventario.model.MovimientoInventario> movCaptor = ArgumentCaptor.forClass(com.willyes.clemenintegra.inventario.model.MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getAlmacenDestino().getId()).isEqualTo(2L);
        assertThat(movCaptor.getValue().getAlmacenOrigen().getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Debe liberar lote de PS trasladándolo a su almacén principal")
    void liberarLoteProductoSemielaborado() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        LoteProducto lote = loteEnCuarentena(20L, producto, 7, new BigDecimal("3.25"));

        mockCatalogosBasicos(13L, 12L);
        when(catalogResolver.resolveAlmacenPrincipal(producto)).thenReturn(8L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(loteProductoRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(20L)).thenReturn(evaluacionesConforme());
        when(almacenRepo.findById(8L)).thenReturn(Optional.of(almacenConId(8)));

        service.liberarLotePorCalidad(20L, jefeCalidad);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(8L);

        ArgumentCaptor<com.willyes.clemenintegra.inventario.model.MovimientoInventario> movCaptor = ArgumentCaptor.forClass(com.willyes.clemenintegra.inventario.model.MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getAlmacenDestino().getId()).isEqualTo(8L);
        assertThat(movCaptor.getValue().getAlmacenOrigen().getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Crear lote sin análisis de calidad lo deja DISPONIBLE")
    void crearLoteSinAnalisisQuedaDisponible() {
        LoteProductoRequestDTO request = LoteProductoRequestDTO.builder()
                .productoId(1L)
                .almacenId(2L)
                .codigoLote("L-001")
                .stockLote(BigDecimal.ONE)
                .fechaVencimiento(LocalDateTime.now().plusDays(30))
                .build();

        Producto producto = new Producto();
        producto.setId(1);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.NINGUNO);
        Almacen almacen = almacenConId(2);
        Usuario usuario = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);

        LoteProducto entidad = new LoteProducto();

        when(productoRepo.findById(1L)).thenReturn(Optional.of(producto));
        when(almacenRepo.findById(2L)).thenReturn(Optional.of(almacen));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(loteProductoMapper.toEntity(request, producto, almacen, usuario)).thenReturn(entidad);
        when(loteProductoRepository.saveAndFlush(entidad)).thenReturn(entidad);
        when(loteProductoMapper.toResponseDTO(entidad)).thenAnswer(inv -> LoteProductoResponseDTO.builder()
                .estado(entidad.getEstado())
                .build());

        LoteProductoResponseDTO respuesta = service.crearLote(request);

        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.DISPONIBLE);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoLote.DISPONIBLE);
    }

    @Test
    @DisplayName("Crear y liberar lote con análisis cambia de CUARENTENA a LIBERADO")
    void crearYLiberarLoteConAnalisis() {
        LoteProductoRequestDTO request = LoteProductoRequestDTO.builder()
                .productoId(5L)
                .almacenId(9L)
                .codigoLote("L-002")
                .stockLote(BigDecimal.TEN)
                .fechaVencimiento(LocalDateTime.now().plusDays(60))
                .build();

        Producto producto = new Producto();
        producto.setId(5);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.FISICO);
        Almacen almacen = almacenConId(9);
        Usuario usuario = usuarioConRol(RolUsuario.ROL_ANALISTA_CALIDAD);

        LoteProducto entidad = LoteProducto.builder()
                .id(44L)
                .estado(null)
                .stockReservado(BigDecimal.ZERO)
                .build();

        when(productoRepo.findById(5L)).thenReturn(Optional.of(producto));
        when(almacenRepo.findById(9L)).thenReturn(Optional.of(almacen));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(loteProductoMapper.toEntity(request, producto, almacen, usuario)).thenReturn(entidad);
        when(loteProductoRepository.saveAndFlush(entidad)).thenReturn(entidad);
        when(loteProductoMapper.toResponseDTO(entidad)).thenAnswer(inv -> LoteProductoResponseDTO.builder()
                .estado(entidad.getEstado())
                .build());
        when(loteProductoRepository.findById(44L)).thenReturn(Optional.of(entidad));
        when(evaluacionRepository.findByLoteProductoId(44L)).thenReturn(evaluacionesConforme());

        LoteProductoResponseDTO creado = service.crearLote(request);

        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
        assertThat(creado.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);

        LoteProductoResponseDTO liberado = service.liberarLote(44L);

        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(entidad.getFechaLiberacion()).isNotNull();
        assertThat(entidad.getUsuarioLiberador()).isEqualTo(usuario);
        assertThat(liberado.getEstado()).isEqualTo(EstadoLote.LIBERADO);
    }

    private void mockCatalogosBasicos(Long motivoId, Long tipoDetalleId) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(motivoId);
        motivo.setMotivo(ClasificacionMovimientoInventario.LIBERACION_CALIDAD);
        when(catalogResolver.getMotivoIdTransferenciaCalidad()).thenReturn(motivoId);
        when(motivoMovimientoRepository.findById(motivoId)).thenReturn(Optional.of(motivo));

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleId);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(tipoDetalleId);
        when(tipoMovimientoDetalleRepository.findById(tipoDetalleId)).thenReturn(Optional.of(tipoDetalle));
    }

    private List<EvaluacionCalidad> evaluacionesConforme() {
        return List.of(
                EvaluacionCalidad.builder()
                        .resultado(ResultadoEvaluacion.CONFORME)
                        .tipoEvaluacion(TipoEvaluacion.FISICO)
                        .observaciones("ok")
                        .build(),
                EvaluacionCalidad.builder()
                        .resultado(ResultadoEvaluacion.CONFORME)
                        .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                        .observaciones("ok")
                        .build()
        );
    }

    private Usuario usuarioConRol(RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setRol(rol);
        return usuario;
    }

    private Producto productoConCategoria(TipoCategoria tipoCategoria) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(5L);
        categoria.setTipo(tipoCategoria);

        Producto producto = new Producto();
        producto.setId(3);
        producto.setCategoriaProducto(categoria);
        producto.setTipoAnalisisCalidad(TipoAnalisisCalidad.AMBOS);
        return producto;
    }

    private LoteProducto loteEnCuarentena(Long id, Producto producto, Integer almacenId, BigDecimal stock) {
        Almacen almacen = almacenConId(almacenId);
        return LoteProducto.builder()
                .id(id)
                .producto(producto)
                .almacen(almacen)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(stock)
                .build();
    }

    private Almacen almacenConId(Integer id) {
        Almacen almacen = new Almacen();
        almacen.setId(id);
        return almacen;
    }
}

