package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.calidad.mapper.CondicionUsoMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.CondicionUsoRepository;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;
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
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock private PlantillaAnalisisMicroService plantillaAnalisisMicroService;
    @Mock private CondicionUsoRepository condicionUsoRepository;
    @Mock private CondicionUsoMapper condicionUsoMapper;
    @Mock private BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    @Mock private ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;

    @InjectMocks
    private LoteProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "estadoLiberadoConf", "LIBERADO");
        ReflectionTestUtils.setField(service, "clasificacionLiberacionConf", "LIBERACION_CALIDAD");
        ReflectionTestUtils.setField(service, "clasificacionRechazoCalidad", "RECHAZO_CALIDAD");

        when(retencionLoteService.obtenerRetencionesActivas(anyLong())).thenReturn(Collections.emptyList());
        when(noConformidadService.obtenerActivaPorLote(anyLong())).thenReturn(Optional.empty());
        when(condicionUsoService.getActivasByLote(anyLong())).thenReturn(Collections.emptyList());
        when(movimientoInventarioRepository.existsByTipoMovimientoAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoIdAndClasificacion(
                any(), anyLong(), anyLong(), anyLong(), any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe liberar lote de PT usando su almacén principal")
    void liberarLoteProductoTerminado() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(true);
        producto.recomputarTipoAnalisisDesdeBanderas();
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
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(true);
        producto.recomputarTipoAnalisisDesdeBanderas();
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
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
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
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        Almacen almacen = almacenConId(9);
        Usuario usuario = usuarioConRol(RolUsuario.ROL_ANALISTA_CALIDAD);

        LoteProducto entidad = LoteProducto.builder()
                .id(44L)
                .estado(null)
                .stockLote(BigDecimal.TEN)
                .producto(producto)
                .almacen(almacen)
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
        mockCatalogosBasicos(13L, 12L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(9L);
        when(catalogResolver.resolveAlmacenPrincipal(producto)).thenReturn(2L);
        when(loteProductoRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(entidad));
        when(evaluacionRepository.findByLoteProductoId(44L)).thenReturn(evaluacionesConforme());
        when(almacenRepo.findById(2L)).thenReturn(Optional.of(almacenConId(2)));

        LoteProductoResponseDTO creado = service.crearLote(request);

        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
        assertThat(creado.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);

        LoteProductoResponseDTO liberado = service.liberarLote(44L);

        assertThat(entidad.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(entidad.getFechaLiberacion()).isNotNull();
        assertThat(entidad.getUsuarioLiberador()).isEqualTo(usuario);
        assertThat(entidad.getAlmacen().getId()).isEqualTo(2L);
        assertThat(liberado.getEstado()).isEqualTo(EstadoLote.LIBERADO);
    }

    @Test
    @DisplayName("Libera lote cuando existen resultados micro y PDF requeridos")
    void liberarLoteConMicroCompleto() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);
        producto.recomputarTipoAnalisisDesdeBanderas();

        LoteProducto lote = loteEnCuarentena(30L, producto, 7, BigDecimal.ONE);
        lote.setStockReservado(BigDecimal.ZERO);

        EvaluacionCalidad evaluacionMicro = evaluacionMicroCompleta(300L, lote);

        mockCatalogosBasicos(13L, 12L);
        when(catalogResolver.resolveAlmacenPrincipal(producto)).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(loteProductoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(30L)).thenReturn(List.of(evaluacionMicro));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(any()))
                .thenReturn(List.of(com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico.builder()
                        .id(1L)
                        .evaluacion(evaluacionMicro)
                        .build()));
        when(almacenRepo.findById(2L)).thenReturn(Optional.of(almacenConId(2)));

        service.liberarLotePorCalidad(30L, jefeCalidad);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.LIBERADO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Rechazar lote desde almacén normal mueve a rechazo y registra movimiento")
    void rechazarLote_debeRechazarDesdeAlmacenNormal() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        LoteProducto lote = loteEnCuarentena(90L, producto, 5, new BigDecimal("10"));
        lote.setStockReservado(BigDecimal.ZERO);

        MotivoMovimiento motivo = mockMotivoRechazo(21L);
        TipoMovimientoDetalle tipoDetalle = mockTipoDetalleTransferencia(22L);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(jefeCalidad);
        when(loteProductoRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(90L)).thenReturn(evaluacionesConforme());
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(99L);
        when(almacenRepo.findById(99L)).thenReturn(Optional.of(almacenConId(99)));
        when(loteProductoMapper.toResponseDTO(any())).thenReturn(LoteProductoResponseDTO.builder()
                .estado(EstadoLote.RECHAZADO)
                .build());

        service.rechazarLote(90L);

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.RECHAZADO);
        assertThat(lote.getAlmacen().getId()).isEqualTo(99);

        ArgumentCaptor<com.willyes.clemenintegra.inventario.model.MovimientoInventario> movCaptor =
                ArgumentCaptor.forClass(com.willyes.clemenintegra.inventario.model.MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getAlmacenOrigen().getId()).isEqualTo(5);
        assertThat(movCaptor.getValue().getAlmacenDestino().getId()).isEqualTo(99);
        assertThat(movCaptor.getValue().getMotivoMovimiento()).isSameAs(motivo);
        assertThat(movCaptor.getValue().getTipoMovimientoDetalle()).isSameAs(tipoDetalle);
        assertThat(movCaptor.getValue().getClasificacion()).isEqualTo(ClasificacionMovimientoInventario.RECHAZO_CALIDAD);
    }

    @Test
    @DisplayName("Rechazar lote falla si ya está en almacén de rechazo")
    void rechazarLote_debeFallarSiYaEstaEnAlmacenRechazo() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        LoteProducto lote = loteEnCuarentena(91L, producto, 99, new BigDecimal("5"));
        lote.setStockReservado(BigDecimal.ZERO);

        mockMotivoRechazo(21L);
        mockTipoDetalleTransferencia(22L);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(jefeCalidad);
        when(loteProductoRepository.findByIdForUpdate(91L)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(91L)).thenReturn(evaluacionesConforme());
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(99L);
        when(almacenRepo.findById(99L)).thenReturn(Optional.of(almacenConId(99)));

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> service.rechazarLote(91L));

        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO);
    }

    @Test
    @DisplayName("Rechazar lote falla si no tiene almacén asociado")
    void rechazarLote_debeFallarSiAlmacenNull() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        LoteProducto lote = LoteProducto.builder()
                .id(92L)
                .producto(producto)
                .almacen(null)
                .estado(EstadoLote.EN_CUARENTENA)
                .stockLote(new BigDecimal("5"))
                .stockReservado(BigDecimal.ZERO)
                .build();

        mockMotivoRechazo(21L);
        mockTipoDetalleTransferencia(22L);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(jefeCalidad);
        when(loteProductoRepository.findByIdForUpdate(92L)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(92L)).thenReturn(evaluacionesConforme());
        when(catalogResolver.getAlmacenObsoletosId()).thenReturn(99L);

        CustomBusinessException ex = assertThrows(CustomBusinessException.class,
                () -> service.rechazarLote(92L));

        assertThat(ex.getCode()).isEqualTo(ApiErrorCode.LOTE_EN_ALMACEN_INVALIDO_PARA_RECHAZO);
    }

    @Test
    @DisplayName("Reabrir lote crea retención de reevaluación y mantiene trazabilidad")
    void reabrirLoteCreaRetencionReevaluacion() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        LoteProducto lote = LoteProducto.builder()
                .id(60L)
                .estado(EstadoLote.LIBERADO)
                .producto(producto)
                .almacen(almacenConId(2))
                .build();

        when(loteProductoRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(lote));
        when(loteProductoRepository.findById(60L)).thenReturn(Optional.of(lote));

        com.willyes.clemenintegra.calidad.dto.ReaperturaLoteRequestDTO dto =
                com.willyes.clemenintegra.calidad.dto.ReaperturaLoteRequestDTO.builder()
                        .motivo("Reevaluación por desviación")
                        .build();

        service.reabrirParaReevaluacion(60L, dto, jefeCalidad);

        verify(retencionLoteService).retenerLote(
                60L,
                com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion.REEVALUACION,
                "Reevaluación por desviación",
                null,
                jefeCalidad,
                true);
    }

    @Test
    @DisplayName("Lista por evaluar mantiene lote F+M hasta completar microbiológico")
    void listarPorEvaluarMantieneLoteFisicoMicroIncompleto() {
        mockAuthWithRoles("ROL_SUPER_ADMIN");

        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(true);
        producto.recomputarTipoAnalisisDesdeBanderas();

        LoteProducto lote = loteEnCuarentena(40L, producto, 7, BigDecimal.ONE);

        EvaluacionCalidad evalFisico = evaluacionFisica(400L, lote);
        EvaluacionCalidad evalQM = evaluacionQuimicoMicro(401L, lote);

        when(loteProductoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(lote), List.of(lote), List.of(lote));
        when(evaluacionRepository.findByLoteProductoId(40L))
                .thenReturn(List.of(), List.of(evalFisico), List.of(evalFisico, evalQM));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(anyList()))
                .thenReturn(List.of(com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico.builder()
                        .id(1L)
                        .evaluacion(evalQM)
                        .build()));
        when(loteProductoMapper.toDto(any())).thenAnswer(inv -> LoteProductoResponseDTO.builder()
                .id(((LoteProducto) inv.getArgument(0)).getId())
                .build());
        when(plantillaAnalisisMicroService.obtenerPorProducto(anyLong())).thenReturn(null);

        Page<LoteProductoResponseDTO> sinEvaluaciones = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(sinEvaluaciones.getContent()).hasSize(1);
        assertThat(sinEvaluaciones.getContent().get(0).isPendienteMicro()).isTrue();

        Page<LoteProductoResponseDTO> conFisico = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(conFisico.getContent()).hasSize(1);
        assertThat(conFisico.getContent().get(0).isPendienteMicro()).isTrue();

        Page<LoteProductoResponseDTO> completo = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(completo.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Lista por evaluar incluye lote Q+M sin resultados micro y lo excluye al completarlos")
    void listarPorEvaluarLoteQuimicoMicro() {
        mockAuthWithRoles("ROL_SUPER_ADMIN");

        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);
        producto.recomputarTipoAnalisisDesdeBanderas();

        LoteProducto lote = loteEnCuarentena(41L, producto, 7, BigDecimal.ONE);
        EvaluacionCalidad evalQM = evaluacionQuimicoMicro(402L, lote);

        when(loteProductoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(lote), List.of(lote));
        when(evaluacionRepository.findByLoteProductoId(41L))
                .thenReturn(List.of(evalQM), List.of(evalQM));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(anyList()))
                .thenReturn(Collections.emptyList(), List.of(com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico.builder()
                        .id(2L)
                        .evaluacion(evalQM)
                        .build()));
        when(loteProductoMapper.toDto(any())).thenAnswer(inv -> LoteProductoResponseDTO.builder()
                .id(((LoteProducto) inv.getArgument(0)).getId())
                .build());
        when(plantillaAnalisisMicroService.obtenerPorProducto(anyLong())).thenReturn(null);

        Page<LoteProductoResponseDTO> sinResultados = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(sinResultados.getContent()).hasSize(1);
        assertThat(sinResultados.getContent().get(0).isPendienteMicro()).isTrue();

        Page<LoteProductoResponseDTO> conResultados = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(conResultados.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Lista por evaluar excluye lote solo físico cuando ya tiene evaluación física")
    void listarPorEvaluarLoteSoloFisico() {
        mockAuthWithRoles("ROL_SUPER_ADMIN");

        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();

        LoteProducto lote = loteEnCuarentena(42L, producto, 7, BigDecimal.ONE);
        EvaluacionCalidad evalFisico = evaluacionFisica(403L, lote);

        when(loteProductoRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(lote));
        when(evaluacionRepository.findByLoteProductoId(42L))
                .thenReturn(List.of(evalFisico));
        when(loteProductoMapper.toDto(any())).thenAnswer(inv -> LoteProductoResponseDTO.builder()
                .id(((LoteProducto) inv.getArgument(0)).getId())
                .build());
        when(plantillaAnalisisMicroService.obtenerPorProducto(anyLong())).thenReturn(null);

        Page<LoteProductoResponseDTO> resultado = service.obtenerLotesPorEvaluar(PageRequest.of(0, 10));
        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Bloquea liberación si falta resultados microbiológicos")
    void bloqueaLiberacionSinResultadosMicro() {
        Usuario jefeCalidad = usuarioConRol(RolUsuario.ROL_JEFE_CALIDAD);
        Producto producto = productoConCategoria(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        LoteProducto lote = loteEnCuarentena(31L, producto, 7, BigDecimal.ONE);
        lote.setStockReservado(BigDecimal.ZERO);

        EvaluacionCalidad evaluacionMicro = evaluacionMicroCompleta(301L, lote);

        mockCatalogosBasicos(13L, 12L);
        when(catalogResolver.resolveAlmacenPrincipal(producto)).thenReturn(2L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(7L);
        when(loteProductoRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(lote));
        when(evaluacionRepository.findByLoteProductoId(31L)).thenReturn(List.of(evaluacionMicro));
        when(resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(any()))
                .thenReturn(Collections.emptyList());

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> service.liberarLotePorCalidad(31L, jefeCalidad));

        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(ex.getReason()).isEqualTo("Faltan resultados microbiológicos");
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

    private MotivoMovimiento mockMotivoRechazo(Long motivoId) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(motivoId);
        motivo.setMotivo(ClasificacionMovimientoInventario.RECHAZO_CALIDAD);
        when(catalogResolver.getMotivoIdAjusteRechazo()).thenReturn(motivoId);
        when(motivoMovimientoRepository.findById(motivoId)).thenReturn(Optional.of(motivo));
        return motivo;
    }

    private TipoMovimientoDetalle mockTipoDetalleTransferencia(Long tipoDetalleId) {
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleId);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(tipoDetalleId);
        when(tipoMovimientoDetalleRepository.findById(tipoDetalleId)).thenReturn(Optional.of(tipoDetalle));
        return tipoDetalle;
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
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
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

    private EvaluacionCalidad evaluacionMicroCompleta(Long id, LoteProducto lote) {
        return EvaluacionCalidad.builder()
                .id(id)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("ok")
                .loteProducto(lote)
                .archivosAdjuntos(List.of(com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion.builder()
                        .nombreVisible(com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO)
                        .nombreArchivo("micro.pdf")
                        .build()))
                .build();
    }

    private EvaluacionCalidad evaluacionFisica(Long id, LoteProducto lote) {
        return EvaluacionCalidad.builder()
                .id(id)
                .tipoEvaluacion(TipoEvaluacion.FISICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("ok")
                .loteProducto(lote)
                .build();
    }

    private EvaluacionCalidad evaluacionQuimicoMicro(Long id, LoteProducto lote) {
        return EvaluacionCalidad.builder()
                .id(id)
                .tipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .resultado(ResultadoEvaluacion.CONFORME)
                .observaciones("ok")
                .loteProducto(lote)
                .build();
    }

    private void mockAuthWithRoles(String... roles) {
        var authorities = Stream.of(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "pass", authorities));
    }
}
