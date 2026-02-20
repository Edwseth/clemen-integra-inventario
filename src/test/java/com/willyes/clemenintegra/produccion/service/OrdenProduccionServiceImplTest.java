package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.dto.CrearOrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.OpHomeopaticoOverride;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenProduccionServiceImplTest {

    @Mock private FormulaProductoRepository formulaProductoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SolicitudMovimientoService solicitudMovimientoService;
    @Mock private OrdenProduccionRepository ordenProduccionRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private CierreProduccionRepository cierreProduccionRepository;
    @Mock private MovimientoInventarioService movimientoInventarioService;
    @Mock private LoteProductoRepository loteProductoRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private UnidadConversionService unidadConversionService;
    @Mock private EtapaProduccionRepository etapaProduccionRepository;
    @Mock private EtapaPlantillaRepository etapaPlantillaRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private UmValidator umValidator;
    @Mock private VidaUtilProductoService vidaUtilProductoService;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private ReservaLoteRepository reservaLoteRepository;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;
    @Mock private ChecklistEtapaService checklistEtapaService;
    @Mock private LoteConsecutivoDiaService loteConsecutivoDiaService;
    @Mock private OpHomeopaticoOverrideRepository opHomeopaticoOverrideRepository;
    @Mock private RegularizacionTrazabilidadRepository regularizacionTrazabilidadRepository;

    @Spy
    @InjectMocks
    private OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(service).reservarInsumosParaOP(anyLong(), any());
        lenient().doNothing().when(checklistEtapaService).validarChecklistCompleto(anyLong());
        lenient().when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> {
            OrdenProduccion op = invocation.getArgument(0);
            if (op.getId() == null) {
                op.setId(100L);
            }
            return op;
        });
        lenient().when(ordenProduccionRepository.countByCodigoOrdenStartingWith(any())).thenReturn(0L);
        lenient().when(ordenProduccionRepository.findCodigosByPrefijo(any())).thenReturn(List.of());
        lenient().when(regularizacionTrazabilidadRepository.existsByOrdenProduccionId(anyLong())).thenReturn(false);
        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .id(1L)
                .nombre("Preparación")
                .secuencia(1)
                .build();
        lenient().when(etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(anyInt()))
                .thenReturn(List.of(etapa));
        lenient().when(etapaProduccionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "EJECUTADA");
        ReflectionTestUtils.setField(service, "clasificacionEntradaPtConf", "ENTRADA_PRODUCTO_TERMINADO");
        lenient().when(umValidator.ajustar(any(BigDecimal.class)))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).setScale(2, RoundingMode.HALF_UP));
        lenient().when(umValidator.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        lenient().when(catalogResolver.decimals(any())).thenReturn(2);
        lenient().when(catalogResolver.getTipoDetalleSalidaProduccionId()).thenReturn(11L);
        lenient().when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(11L);
        lenient().when(catalogResolver.getMotivoSalidaProduccionId()).thenReturn(11L);
        lenient().when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);
        lenient().when(loteConsecutivoDiaService.obtenerSiguienteConsecutivo(any(LocalDate.class))).thenReturn(1);
        TipoMovimientoDetalle tipoSalida = new TipoMovimientoDetalle();
        tipoSalida.setId(11L);
        lenient().when(tipoMovimientoDetalleRepository.findById(11L)).thenReturn(Optional.of(tipoSalida));
        MotivoMovimiento motivoSalida = new MotivoMovimiento();
        motivoSalida.setId(11L);
        lenient().when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivoSalida));
        lenient().when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(anyLong(), anyLong(), any(), anyLong()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("generarCodigoOrden usa el máximo consecutivo del día")
    void generarCodigoOrden_usaMaximoConsecutivoDelDia() {
        String prefijo = "OP-CLEMEN-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        when(ordenProduccionRepository.findCodigosByPrefijo(prefijo))
                .thenReturn(List.of(
                        prefijo + "-01",
                        prefijo + "-04",
                        prefijo + "-02"
                ));

        String codigo = ReflectionTestUtils.invokeMethod(service, "generarCodigoOrden");

        assertThat(codigo).isEqualTo(prefijo + "-05");
    }

    @Test
    @DisplayName("generarCodigoOrden ignora sufijos inválidos")
    void generarCodigoOrden_ignoraSufijosInvalidos() {
        String prefijo = "OP-CLEMEN-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        when(ordenProduccionRepository.findCodigosByPrefijo(prefijo))
                .thenReturn(List.of(
                        prefijo + "-AA",
                        prefijo + "-  ",
                        prefijo + "-03"
                ));

        String codigo = ReflectionTestUtils.invokeMethod(service, "generarCodigoOrden");

        assertThat(codigo).isEqualTo(prefijo + "-04");
    }

    @Test
    @DisplayName("guardarConValidacionStock retorna faltantes y max producible cuando stock es insuficiente")
    void guardarConValidacionStock_retornaFaltantes() {
        Producto producto = new Producto();
        producto.setId(1);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        producto.setUnidadMedida(unidad);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("10"));
        orden.setEstado(EstadoProduccion.CREADA);

        Producto insumo = new Producto();
        insumo.setId(2);
        insumo.setNombre("Extracto X");
        UnidadMedida unidadInsumo = new UnidadMedida();
        unidadInsumo.setSimbolo("kg");
        insumo.setUnidadMedida(unidadInsumo);
        CategoriaProducto categoriaInsumo = new CategoriaProducto();
        categoriaInsumo.setTipo(TipoCategoria.MATERIA_PRIMA);
        insumo.setCategoriaProducto(categoriaInsumo);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(BigDecimal.ONE);

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(1L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any()))
                .thenReturn(List.of(insumo));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo)).thenReturn(List.of(5L));

        DistribucionFefoResult preview = DistribucionFefoResult.builder()
                .productoInsumoId(2L)
                .requerido(new BigDecimal("10.000000"))
                .stockFisicoTotal(new BigDecimal("12.000000"))
                .stockReservadoTotal(new BigDecimal("4.000000"))
                .stockLibreTotal(new BigDecimal("8.000000"))
                .faltante(new BigDecimal("2.000000"))
                .suficiente(false)
                .almacenesPreferidos(List.of(5L))
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(2L), any(BigDecimal.class), eq(List.of(5L)), eq(true)))
                .thenReturn(preview);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isFalse();
        assertThat(resultado.getUnidadesMaximasProducibles()).isEqualTo(8);
        assertThat(resultado.getInsumosFaltantes()).hasSize(1);
        assertThat(resultado.getInsumosFaltantes().get(0).getProductoId()).isEqualTo(2L);
        assertThat(resultado.getInsumosFaltantes().get(0).getRequerido()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(resultado.getInsumosFaltantes().get(0).getDisponible()).isEqualByComparingTo(new BigDecimal("8.000000"));
    }

    @Test
    @DisplayName("crearOrden valida rendimiento obligatorio para PS")
    void crearOrden_conPsSinRendimientoLanzaExcepcion() {
        Producto producto = new Producto();
        producto.setId(20);
        producto.setCodigoSku("PS-001");
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        producto.setCategoriaProducto(categoria);
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setSimbolo("UND");
        producto.setUnidadMedida(unidadMedida);

        when(productoRepository.findById(20L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(new Usuario()));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(20L);
        dto.setResponsableId(5L);
        dto.setCantidadProgramada(new BigDecimal("10"));
        dto.setUnidadMedidaSimbolo("UND");

        assertThatThrownBy(() -> service.crearOrden(dto))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("rendimiento por unidad definido y mayor que cero");
    }

    @Test
    @DisplayName("crearOrden usa rendimiento válido para PS")
    void crearOrden_conPsValidoUtilizaRendimiento() {
        Producto producto = new Producto();
        producto.setId(21);
        producto.setCodigoSku("PS-002");
        producto.setRendimientoUnidad(new BigDecimal("8"));
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        producto.setCategoriaProducto(categoria);
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setSimbolo("UND");
        producto.setUnidadMedida(unidadMedida);

        when(productoRepository.findById(21L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(new Usuario()));
        when(unidadConversionService.convertir(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("10"));
        when(unidadConversionService.dividirNormalizado(any(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("1.25"));
        doReturn(ResultadoValidacionOrdenDTO.builder().esValida(true).build())
                .when(service).guardarConValidacionStock(any(OrdenProduccion.class));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(21L);
        dto.setResponsableId(6L);
        dto.setCantidadProgramada(new BigDecimal("10"));
        dto.setUnidadMedidaSimbolo("UND");

        service.crearOrden(dto);

        verify(unidadConversionService).dividirNormalizado(
                new BigDecimal("10"),
                "UND",
                new BigDecimal("8"),
                "UND");
    }

    @Test
    @DisplayName("crearOrden exige confirmación para homeopático con cantidad > 30")
    void crearOrden_homeopaticoSinConfirmacion_rechaza() {
        Producto producto = productoFabricable(31, TipoCategoria.PRODUCTO_TERMINADO, "UND");
        when(productoRepository.findById(31L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(new Usuario()));
        when(unidadConversionService.convertir(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("200"));
        when(vidaUtilProductoService.buscarPorProductoId(31)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(31)
                .semanasVigencia(78)
                .build()));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(31L);
        dto.setResponsableId(9L);
        dto.setCantidadProgramada(new BigDecimal("200"));
        dto.setUnidadMedidaSimbolo("UND");

        assertThatThrownBy(() -> service.crearOrden(dto))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException cbe = (CustomBusinessException) ex;
                    assertThat(cbe.getCode()).isEqualTo(ApiErrorCode.OP_HOMEOPATICO_REQUIERE_CONFIRMACION);
                    assertThat(cbe.getDetails()).isInstanceOf(Map.class);
                    Map<?, ?> details = (Map<?, ?>) cbe.getDetails();
                    assertThat(details.get("semanasVigencia")).isEqualTo(78);
                    assertThat(details.get("maxRecomendado")).isEqualTo(new BigDecimal("30"));
                });

        verify(service, never()).guardarConValidacionStock(any(OrdenProduccion.class));
        verify(opHomeopaticoOverrideRepository, never()).save(any(OpHomeopaticoOverride.class));
    }

    @Test
    @DisplayName("crearOrden homeopático confirmado registra auditoría")
    void crearOrden_homeopaticoConfirmado_registraAuditoria() {
        Producto producto = productoFabricable(32, TipoCategoria.PRODUCTO_TERMINADO, "UND");
        when(productoRepository.findById(32L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(new Usuario()));
        when(unidadConversionService.convertir(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("200"));
        when(unidadConversionService.dividirNormalizado(any(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("200"));
        when(vidaUtilProductoService.buscarPorProductoId(32)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(32)
                .semanasVigencia(78)
                .build()));
        Usuario usuario = new Usuario();
        usuario.setId(77L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        ResultadoValidacionOrdenDTO resultado = ResultadoValidacionOrdenDTO.builder()
                .esValida(true)
                .orden(new OrdenProduccionResponseDTO())
                .build();
        Long versionEsperada = 5L;
        resultado.getOrden().id = 900L;
        doAnswer(invocation -> {
            OrdenProduccion orden = invocation.getArgument(0);
            orden.setId(900L);
            orden.setVersion(versionEsperada);
            return resultado;
        }).when(service).guardarConValidacionStock(any(OrdenProduccion.class));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(32L);
        dto.setResponsableId(10L);
        dto.setCantidadProgramada(new BigDecimal("200"));
        dto.setUnidadMedidaSimbolo("UND");
        dto.setConfirmacionHomeopatico(true);
        dto.setMotivoOverrideHomeopatico("Se requiere este lote para cubrir pedido regulatorio urgente");

        ResultadoValidacionOrdenDTO respuesta = service.crearOrden(dto);
        assertThat(respuesta.isEsValida()).isTrue();

        ArgumentCaptor<OpHomeopaticoOverride> captor = ArgumentCaptor.forClass(OpHomeopaticoOverride.class);
        verify(opHomeopaticoOverrideRepository).save(captor.capture());
        assertThat(captor.getValue().getOrdenProduccion().getId()).isEqualTo(900L);
        assertThat(captor.getValue().getOrdenProduccion().getVersion()).isEqualTo(versionEsperada);
        assertThat(captor.getValue().getProducto().getId()).isEqualTo(32);
        assertThat(captor.getValue().getSemanasVigencia()).isEqualTo(78);

        ArgumentCaptor<OrdenProduccion> ordenCaptor = ArgumentCaptor.forClass(OrdenProduccion.class);
        verify(service).guardarConValidacionStock(ordenCaptor.capture());
        assertThat(ordenCaptor.getValue().getConfirmacionHomeopatico()).isTrue();
        assertThat(ordenCaptor.getValue().getMotivoOverrideHomeopatico())
                .isEqualTo("Se requiere este lote para cubrir pedido regulatorio urgente");
    }

    @Test
    @DisplayName("crearOrden homeopático confirmado sin motivo válido rechaza")
    void crearOrden_homeopaticoConfirmadoSinMotivo_rechaza() {
        Producto producto = productoFabricable(33, TipoCategoria.PRODUCTO_TERMINADO, "UND");
        when(productoRepository.findById(33L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(new Usuario()));
        when(unidadConversionService.convertir(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("200"));
        when(vidaUtilProductoService.buscarPorProductoId(33)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(33)
                .semanasVigencia(78)
                .build()));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(33L);
        dto.setResponsableId(11L);
        dto.setCantidadProgramada(new BigDecimal("200"));
        dto.setUnidadMedidaSimbolo("UND");
        dto.setConfirmacionHomeopatico(true);
        dto.setMotivoOverrideHomeopatico("motivo corto");

        assertThatThrownBy(() -> service.crearOrden(dto))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(ex -> {
                    CustomBusinessException cbe = (CustomBusinessException) ex;
                    assertThat(cbe.getCode()).isEqualTo(ApiErrorCode.OP_HOMEOPATICO_MOTIVO_OBLIGATORIO);
                });

        verify(service, never()).guardarConValidacionStock(any(OrdenProduccion.class));
        verify(opHomeopaticoOverrideRepository, never()).save(any(OpHomeopaticoOverride.class));
    }

    @Test
    @DisplayName("crearOrden normal no exige confirmación y no audita")
    void crearOrden_noHomeopatico_noExigeConfirmacion() {
        Producto producto = productoFabricable(34, TipoCategoria.PRODUCTO_TERMINADO, "UND");
        when(productoRepository.findById(34L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(new Usuario()));
        when(unidadConversionService.convertir(any(BigDecimal.class), any(), any())).thenReturn(new BigDecimal("200"));
        when(unidadConversionService.dividirNormalizado(any(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("200"));
        when(vidaUtilProductoService.buscarPorProductoId(34)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(34)
                .semanasVigencia(12)
                .build()));
        doReturn(ResultadoValidacionOrdenDTO.builder().esValida(true).build())
                .when(service).guardarConValidacionStock(any(OrdenProduccion.class));

        CrearOrdenProduccionRequestDTO dto = new CrearOrdenProduccionRequestDTO();
        dto.setProductoId(34L);
        dto.setResponsableId(12L);
        dto.setCantidadProgramada(new BigDecimal("200"));
        dto.setUnidadMedidaSimbolo("UND");

        ResultadoValidacionOrdenDTO respuesta = service.crearOrden(dto);
        assertThat(respuesta.isEsValida()).isTrue();
        verify(service).guardarConValidacionStock(any(OrdenProduccion.class));
        verify(opHomeopaticoOverrideRepository, never()).save(any(OpHomeopaticoOverride.class));
    }

    private Producto productoFabricable(int id, TipoCategoria tipoCategoria, String simboloUm) {
        Producto producto = new Producto();
        producto.setId(id);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipoCategoria);
        producto.setCategoriaProducto(categoria);
        UnidadMedida um = new UnidadMedida();
        um.setSimbolo(simboloUm);
        producto.setUnidadMedida(um);
        producto.setRendimientoUnidad(BigDecimal.ONE);
        return producto;
    }

    @Test
    @DisplayName("guardarConValidacionStock aprueba Jarabe Base cuando la simulación FEFO es suficiente")
    void guardarConValidacionStock_jarabeBaseSuficiente() {
        Producto producto = new Producto();
        producto.setId(10);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("UND");
        producto.setUnidadMedida(unidad);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("700"));
        orden.setEstado(EstadoProduccion.CREADA);

        Producto jarabe = new Producto();
        jarabe.setId(38);
        jarabe.setNombre("JARABE BASE");
        jarabe.setCodigoSku("MP-JARBA");
        UnidadMedida unidadMl = new UnidadMedida();
        unidadMl.setSimbolo("ML");
        jarabe.setUnidadMedida(unidadMl);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        jarabe.setCategoriaProducto(categoria);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(jarabe);
        detalle.setCantidadNecesaria(new BigDecimal("199.5"));

        FormulaProducto formula = new FormulaProducto();
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalle));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(jarabe));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(jarabe)).thenReturn(List.of(5L));

        DistribucionFefoResult jarabeDistribucion = crearDistribucionJarabe();
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(38L), any(BigDecimal.class), eq(List.of(5L)), eq(true)))
                .thenReturn(jarabeDistribucion);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isTrue();
        assertThat(resultado.getOrden()).isNotNull();
        assertThat(resultado.getInsumosFaltantes()).isNull();
    }

    @Test
    @DisplayName("guardarConValidacionStock permite insumos SIN_CONTROL_STOCK aunque la simulación no tenga stock")
    void guardarConValidacionStock_insumoSinControlStock() {
        Producto producto = new Producto();
        producto.setId(20);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("LTS");
        producto.setUnidadMedida(unidad);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("5"));
        orden.setEstado(EstadoProduccion.CREADA);

        Producto agua = new Producto();
        agua.setId(21);
        agua.setNombre("AGUA PURIFICADA");
        UnidadMedida umAgua = new UnidadMedida();
        umAgua.setSimbolo("LTS");
        agua.setUnidadMedida(umAgua);
        agua.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(agua);
        detalle.setCantidadNecesaria(BigDecimal.ONE);

        FormulaProducto formula = new FormulaProducto();
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalle));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(20L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(agua));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(agua)).thenReturn(List.of());

        DistribucionFefoResult sinStock = DistribucionFefoResult.builder()
                .productoInsumoId(21L)
                .requerido(new BigDecimal("5.000000"))
                .stockLibreTotal(BigDecimal.ZERO.setScale(6))
                .faltante(new BigDecimal("5.000000"))
                .suficiente(false)
                .build();
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(21L), any(BigDecimal.class), eq(List.of()), eq(true)))
                .thenReturn(sinStock);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isTrue();
        assertThat(resultado.getInsumosFaltantes()).isNull();
    }

    @Test
    @DisplayName("guardarConValidacionStock permite crear la OP cuando el producto tiene etapas activas")
    void guardarConValidacionStock_conEtapasActivas() {
        Producto producto = new Producto();
        producto.setId(5);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        producto.setUnidadMedida(unidad);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("3"));
        orden.setEstado(EstadoProduccion.CREADA);

        Producto insumo = new Producto();
        insumo.setId(9);
        insumo.setNombre("Materia Prima");
        UnidadMedida unidadInsumo = new UnidadMedida();
        unidadInsumo.setSimbolo("kg");
        insumo.setUnidadMedida(unidadInsumo);
        CategoriaProducto categoriaInsumo = new CategoriaProducto();
        categoriaInsumo.setTipo(TipoCategoria.MATERIA_PRIMA);
        insumo.setCategoriaProducto(categoriaInsumo);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(BigDecimal.ONE);

        FormulaProducto formula = new FormulaProducto();
        formula.setProducto(producto);
        formula.setDetalles(List.of(detalle));

        EtapaPlantilla etapa = EtapaPlantilla.builder()
                .id(10L)
                .nombre("Preparación")
                .secuencia(1)
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(5L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(insumo));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(insumo)).thenReturn(List.of(1L));
        when(etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(5))
                .thenReturn(List.of(etapa));

        DistribucionFefoResult disponibilidad = DistribucionFefoResult.builder()
                .productoInsumoId(9L)
                .requerido(new BigDecimal("3.000000"))
                .stockLibreTotal(new BigDecimal("5.000000"))
                .faltante(BigDecimal.ZERO)
                .suficiente(true)
                .build();
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(9L), any(BigDecimal.class), eq(List.of(1L)), eq(true)))
                .thenReturn(disponibilidad);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isTrue();
        assertThat(resultado.getOrden()).isNotNull();
        verify(ordenProduccionRepository).save(any(OrdenProduccion.class));
        verify(etapaProduccionRepository).saveAll(any());
    }

    @Test
    @DisplayName("guardarConValidacionStock bloquea la creación cuando el producto no tiene etapas")
    void guardarConValidacionStock_sinEtapasActivas() {
        Producto producto = new Producto();
        producto.setId(7);
        producto.setNombre("Producto sin etapas");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("UND");
        producto.setUnidadMedida(unidad);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(producto);
        orden.setCantidadProgramada(BigDecimal.ONE);
        orden.setEstado(EstadoProduccion.CREADA);

        when(etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(7))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.guardarConValidacionStock(orden))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).contains("ORDEN_PRODUCTO_SIN_ETAPAS");
                });

        verify(ordenProduccionRepository, never()).save(any());
        verify(etapaProduccionRepository, never()).saveAll(any());
        verify(reservaLoteService, never()).sincronizarReservasSolicitud(any());
    }

    @Test
    @DisplayName("clonarEtapasParaOrden crea etapas pendientes sin fechas iniciales")
    void clonarEtapasParaOrden_inicializaFechasNulas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1000L);

        EtapaPlantilla e1 = EtapaPlantilla.builder().nombre("Corte").secuencia(1).build();
        EtapaPlantilla e2 = EtapaPlantilla.builder().nombre("Mezcla").secuencia(2).build();

        ArgumentCaptor<List<EtapaProduccion>> captor = ArgumentCaptor.forClass(List.class);
        when(etapaProduccionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.clonarEtapasParaOrden(orden, List.of(e1, e2));

        verify(etapaProduccionRepository).saveAll(captor.capture());
        List<EtapaProduccion> guardadas = captor.getValue();
        assertThat(guardadas).hasSize(2);
        guardadas.forEach(etapa -> {
            assertThat(etapa.getEstado()).isEqualTo(EstadoEtapa.PENDIENTE);
            assertThat(etapa.getFechaInicio()).isNull();
            assertThat(etapa.getFechaFin()).isNull();
            assertThat(etapa.getUsuarioId()).isNull();
            assertThat(etapa.getUsuarioNombre()).isNull();
        });
    }

    @Test
    @DisplayName("iniciarEtapa inicia la primera etapa cuando las solicitudes de movimiento están concluidas")
    void iniciarEtapa_conSolicitudesConcluidas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-001");
        orden.setLoteProduccion("L-001");

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(10L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .id(5L)
                .ordenProduccion(orden)
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setNombreCompleto("Operario");

        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(10L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(1L)).thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EtapaProduccion resultado = service.iniciarEtapa(1L, 10L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEtapa.EN_PROCESO);
        assertThat(resultado.getFechaInicio()).isNotNull();
        assertThat(resultado.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(resultado.getUsuarioNombre()).isEqualTo(usuario.getNombreCompleto());
        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
        verify(ordenProduccionRepository).save(orden);
        verify(movimientoInventarioService).consumirInsumosPorOrden(1L, 10L, usuario.getId());
    }

    @Test
    @DisplayName("iniciarEtapa rechaza cuando ya hay otra etapa activa en la OP")
    void iniciarEtapa_otroActivo() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(2L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        EtapaProduccion etapaObjetivo = EtapaProduccion.builder()
                .id(20L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(2)
                .build();

        EtapaProduccion activa = EtapaProduccion.builder()
                .id(21L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusHours(2))
                .build();

        when(ordenProduccionRepository.findById(2L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(20L)).thenReturn(Optional.of(etapaObjetivo));
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(2L))
                .thenReturn(1L);
        when(etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(2L))
                .thenReturn(List.of(activa));

        assertThatThrownBy(() -> service.iniciarEtapa(2L, 20L))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.PRODUCCION_OTRA_ETAPA_ACTIVA);
    }

    @Test
    @DisplayName("iniciarEtapa es idempotente cuando la etapa ya está activa")
    void iniciarEtapa_idempotenteMismaEtapaActiva() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(2L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        EtapaProduccion etapaActiva = EtapaProduccion.builder()
                .id(20L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusMinutes(30))
                .build();

        when(ordenProduccionRepository.findById(2L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(20L)).thenReturn(Optional.of(etapaActiva));
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(2L))
                .thenReturn(1L);
        when(etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(2L))
                .thenReturn(List.of(etapaActiva));

        EtapaProduccion resultado = service.iniciarEtapa(2L, 20L);

        assertThat(resultado).isSameAs(etapaActiva);
        verify(movimientoInventarioService, never()).consumirInsumosPorOrden(any(), any(), any());
    }

    @Test
    @DisplayName("iniciarEtapa devuelve conflicto cuando existen múltiples etapas activas")
    void iniciarEtapa_multiplesActivas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(3L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        EtapaProduccion etapaObjetivo = EtapaProduccion.builder()
                .id(30L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .build();

        EtapaProduccion activa1 = EtapaProduccion.builder().id(31L).ordenProduccion(orden).fechaInicio(LocalDateTime.now().minusHours(2)).build();
        EtapaProduccion activa2 = EtapaProduccion.builder().id(32L).ordenProduccion(orden).fechaInicio(LocalDateTime.now().minusHours(1)).build();

        when(ordenProduccionRepository.findById(3L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(30L)).thenReturn(Optional.of(etapaObjetivo));
        when(etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(3L))
                .thenReturn(2L);
        when(etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(3L))
                .thenReturn(List.of(activa1, activa2));

        assertThatThrownBy(() -> service.iniciarEtapa(3L, 30L))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.PRODUCCION_MULTIPLES_ETAPAS_ACTIVAS);
    }

    @Test
    @DisplayName("iniciarEtapa genera lote de producto terminado usando semanas de vigencia configuradas")
    void iniciarEtapa_conVidaUtilConfigurada() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(5L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-005");

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(15);
        producto.setNombre("PT-Probador");
        producto.setCodigoSku("PT-005");
        producto.setCategoriaProducto(categoria);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(producto);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(15L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setNombreCompleto("Usuario Calidad");

        when(ordenProduccionRepository.findById(5L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(15L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(5L)).thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(15)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(15)
                .producto(producto)
                .semanasVigencia(6)
                .build()));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(99L);
            return lote;
        });
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EtapaProduccion resultado = service.iniciarEtapa(5L, 15L);

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(captor.capture());
        LoteProducto loteGenerado = captor.getValue();

        assertThat(loteGenerado.getFechaVencimiento())
                .isEqualTo(loteGenerado.getFechaFabricacion().plusWeeks(6));
        assertThat(resultado.getEstado()).isEqualTo(EstadoEtapa.EN_PROCESO);
    }

    @Test
    void shouldInheritExpiryFromPS_whenPTIs78WeeksAndPSReservedHasExpiry() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(51L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-051");

        CategoriaProducto categoriaPt = new CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto productoPt = new Producto();
        productoPt.setId(151);
        productoPt.setNombre("PT-78");
        productoPt.setCodigoSku("PT-151");
        productoPt.setCategoriaProducto(categoriaPt);
        productoPt.setRequiereAnalisisFisico(false);
        productoPt.setRequiereAnalisisQuimico(false);
        productoPt.setRequiereAnalisisMicrobiologico(false);
        productoPt.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(productoPt);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(151L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(19L);
        usuario.setNombreCompleto("Usuario PT 78");

        CategoriaProducto categoriaPs = new CategoriaProducto();
        categoriaPs.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        Producto productoPs = new Producto();
        productoPs.setId(251);
        productoPs.setCategoriaProducto(categoriaPs);

        LocalDateTime fechaVencimientoPs = LocalDateTime.now().plusWeeks(8);
        LoteProducto lotePs = new LoteProducto();
        lotePs.setId(701L);
        lotePs.setProducto(productoPs);
        lotePs.setFechaVencimiento(fechaVencimientoPs);

        SolicitudMovimientoDetalle detalle = new SolicitudMovimientoDetalle();
        detalle.setLote(lotePs);
        solicitud.setDetalles(List.of(detalle));

        when(ordenProduccionRepository.findById(51L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(151L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(51L)).thenReturn(List.of(solicitud));
        when(solicitudMovimientoRepository.findWithDetalles(eq(51L), eq(null), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(151)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(151)
                .producto(productoPt)
                .semanasVigencia(78)
                .build()));
        when(loteProductoRepository.findById(701L)).thenReturn(Optional.of(lotePs));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(991L);
            return lote;
        });
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciarEtapa(51L, 151L);

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(captor.capture());
        LoteProducto loteGenerado = captor.getValue();

        assertThat(loteGenerado.getLotePsOrigen()).isNotNull();
        assertThat(loteGenerado.getLotePsOrigen().getId()).isEqualTo(701L);
        assertThat(loteGenerado.getFechaVencimiento()).isEqualTo(fechaVencimientoPs);
    }

    @Test
    void shouldNotInheritExpiryFromPS_whenPTIs104WeeksEvenIfPSReservedHasExpiry() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(52L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-052");

        CategoriaProducto categoriaPt = new CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto productoPt = new Producto();
        productoPt.setId(152);
        productoPt.setNombre("PT-104");
        productoPt.setCodigoSku("PT-152");
        productoPt.setCategoriaProducto(categoriaPt);
        productoPt.setRequiereAnalisisFisico(false);
        productoPt.setRequiereAnalisisQuimico(false);
        productoPt.setRequiereAnalisisMicrobiologico(false);
        productoPt.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(productoPt);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(152L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNombreCompleto("Usuario PT 104");

        when(ordenProduccionRepository.findById(52L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(152L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(52L)).thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(152)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(152)
                .producto(productoPt)
                .semanasVigencia(104)
                .build()));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(992L);
            return lote;
        });
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciarEtapa(52L, 152L);

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(captor.capture());
        LoteProducto loteGenerado = captor.getValue();

        assertThat(loteGenerado.getFechaVencimiento()).isEqualTo(loteGenerado.getFechaFabricacion().plusWeeks(104));
        assertThat(loteGenerado.getLotePsOrigen()).isNull();
        verify(solicitudMovimientoRepository, never())
                .findWithDetalles(eq(52L), eq(null), eq(null), eq(null), eq(false), anyList());
    }

    @Test
    void shouldUsePTExpiry_whenPTIs78WeeksAndNoPSReserved() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(53L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-053");

        CategoriaProducto categoriaPt = new CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto productoPt = new Producto();
        productoPt.setId(153);
        productoPt.setNombre("PT-78-SIN-PS");
        productoPt.setCodigoSku("PT-153");
        productoPt.setCategoriaProducto(categoriaPt);
        productoPt.setRequiereAnalisisFisico(false);
        productoPt.setRequiereAnalisisQuimico(false);
        productoPt.setRequiereAnalisisMicrobiologico(false);
        productoPt.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(productoPt);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(153L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .detalles(List.of())
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(21L);
        usuario.setNombreCompleto("Usuario PT 78 sin PS");

        when(ordenProduccionRepository.findById(53L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(153L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(53L)).thenReturn(List.of(solicitud));
        when(solicitudMovimientoRepository.findWithDetalles(eq(53L), eq(null), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(153)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(153)
                .producto(productoPt)
                .semanasVigencia(78)
                .build()));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(993L);
            return lote;
        });
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciarEtapa(53L, 153L);

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(captor.capture());
        LoteProducto loteGenerado = captor.getValue();

        assertThat(loteGenerado.getFechaVencimiento()).isEqualTo(loteGenerado.getFechaFabricacion().plusWeeks(78));
        assertThat(loteGenerado.getLotePsOrigen()).isNull();
    }

    @Test
    @DisplayName("iniciarEtapa genera lote de producto semielaborado usando semanas de vigencia configuradas")
    void iniciarEtapa_conVidaUtilParaPs() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(6L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-006");

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);

        Producto producto = new Producto();
        producto.setId(25);
        producto.setNombre("PS-Probador");
        producto.setCodigoSku("PS-025");
        producto.setCategoriaProducto(categoria);
        producto.setRequiereAnalisisFisico(true);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(producto);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(16L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Usuario Ps");

        when(ordenProduccionRepository.findById(6L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(16L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(6L)).thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(25)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(25)
                .producto(producto)
                .semanasVigencia(4)
                .build()));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(100L);
            return lote;
        });
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EtapaProduccion resultado = service.iniciarEtapa(6L, 16L);

        ArgumentCaptor<LoteProducto> captor = ArgumentCaptor.forClass(LoteProducto.class);
        verify(loteProductoRepository).save(captor.capture());
        LoteProducto loteGenerado = captor.getValue();

        assertThat(loteGenerado.getFechaVencimiento())
                .isEqualTo(loteGenerado.getFechaFabricacion().plusWeeks(4));
        assertThat(loteGenerado.getEstado()).isEqualTo(EstadoLote.EN_CUARENTENA);
        assertThat(loteGenerado.getAlmacen().getId()).isEqualTo(2L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoEtapa.EN_PROCESO);
    }

    @Test
    @DisplayName("iniciarEtapa bloquea el inicio cuando la OP no tiene solicitudes de movimiento")
    void iniciarEtapa_sinSolicitudes() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(2L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-002");
        orden.setLoteProduccion("L-002");

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(20L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(4L);
        usuario.setNombreCompleto("Supervisor");

        when(ordenProduccionRepository.findById(2L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(20L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(2L)).thenReturn(List.of());
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        assertThatThrownBy(() -> service.iniciarEtapa(2L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).isEqualTo("ORDEN_SIN_SOLICITUDES_MOVIMIENTO");
                });

        verify(etapaProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("finalizarEtapa ejecuta consumo automático antes de cerrar la etapa")
    void finalizarEtapa_disparaConsumo() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(50L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(500L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .ordenProduccion(orden)
                .build();

        Usuario usuario = usuarioBasico();
        usuario.setId(7L);

        when(ordenProduccionRepository.findById(50L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(500L)).thenReturn(Optional.of(etapa));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(etapaProduccionRepository.save(any(EtapaProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(50L))
                .thenReturn(List.of(etapa));
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EtapaProduccion resultado = service.finalizarEtapa(50L, 500L, 7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEtapa.FINALIZADA);
        verify(movimientoInventarioService).consumirInsumosPorOrden(50L, 500L, 7L);
        verify(etapaProduccionRepository).save(any(EtapaProduccion.class));
    }

    @Test
    @DisplayName("fallo en consumo al finalizar etapa revierte el cambio de estado")
    void finalizarEtapa_errorConsumoCancelaCambio() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(60L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(600L)
                .estado(EstadoEtapa.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .ordenProduccion(orden)
                .build();

        Usuario usuario = usuarioBasico();
        usuario.setId(8L);

        when(ordenProduccionRepository.findById(60L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(600L)).thenReturn(Optional.of(etapa));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "SIN_STOCK"))
                .when(movimientoInventarioService).consumirInsumosPorOrden(60L, 600L, 8L);

        assertThatThrownBy(() -> service.finalizarEtapa(60L, 600L, 8L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("reason")
                .isEqualTo("SIN_STOCK");

        assertThat(etapa.getEstado()).isEqualTo(EstadoEtapa.EN_PROCESO);
        verify(etapaProduccionRepository, never()).save(any());
        verify(ordenProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("iniciarEtapa bloquea el inicio cuando existen solicitudes de movimiento pendientes")
    void iniciarEtapa_conSolicitudesPendientes() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(3L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-003");
        orden.setLoteProduccion("L-003");

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(30L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitudPendiente = SolicitudMovimiento.builder()
                .id(8L)
                .ordenProduccion(orden)
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setNombreCompleto("Analista");

        when(ordenProduccionRepository.findById(3L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(30L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(3L)).thenReturn(List.of(solicitudPendiente));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        assertThatThrownBy(() -> service.iniciarEtapa(3L, 30L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).isEqualTo("ORDEN_MOVIMIENTOS_PENDIENTES");
                });

        verify(etapaProduccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("recalcularEstadoOrden marca FINALIZADA cuando las etapas están terminadas y la producción está completa")
    void recalcularEstadoOrden_finalizadaConProduccionCompleta() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(50L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProgramada(new BigDecimal("100"));
        orden.setCantidadProducidaAcumulada(new BigDecimal("100"));
        orden.setTipoCierre(TipoCierre.TOTAL);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(1L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(50L))
                .thenReturn(List.of(etapa));
        when(cierreProduccionRepository.countByOrdenProduccionId(50L)).thenReturn(1L);

        ReflectionTestUtils.invokeMethod(service, "recalcularEstadoOrden", orden);

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.FINALIZADA);
        assertThat(orden.getFechaFin()).isNotNull();
    }

    @Test
    @DisplayName("recalcularEstadoOrden marca CERRADA_INCOMPLETA cuando falta producción pese a etapas finalizadas")
    void recalcularEstadoOrden_cerradaIncompletaCuandoFaltaProduccion() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(60L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProgramada(new BigDecimal("120"));
        orden.setCantidadProducidaAcumulada(new BigDecimal("80"));
        orden.setTipoCierre(TipoCierre.PARCIAL);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(2L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(60L))
                .thenReturn(List.of(etapa));
        when(cierreProduccionRepository.countByOrdenProduccionId(60L)).thenReturn(1L);

        ReflectionTestUtils.invokeMethod(service, "recalcularEstadoOrden", orden);

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.CERRADA_INCOMPLETA);
        assertThat(orden.getFechaFin()).isNotNull();
    }

    @Test
    @DisplayName("recalcularEstadoOrden mantiene EN_PROCESO cuando hay etapas pendientes")
    void recalcularEstadoOrden_conEtapasPendientes() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(70L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProgramada(new BigDecimal("150"));
        orden.setCantidadProducidaAcumulada(new BigDecimal("140"));

        EtapaProduccion finalizada = EtapaProduccion.builder()
                .id(3L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();
        EtapaProduccion pendiente = EtapaProduccion.builder()
                .id(4L)
                .estado(EstadoEtapa.PENDIENTE)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(70L))
                .thenReturn(List.of(finalizada, pendiente));

        ReflectionTestUtils.invokeMethod(service, "recalcularEstadoOrden", orden);

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
        assertThat(orden.getFechaFin()).isNull();
    }

    @Test
    @DisplayName("recalcularEstadoOrden rechaza cierre sin cierres registrados")
    void recalcularEstadoOrden_rechazaSinCierres() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(80L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProgramada(new BigDecimal("50"));
        orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);
        orden.setTipoCierre(TipoCierre.TOTAL);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(5L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(80L))
                .thenReturn(List.of(etapa));
        when(cierreProduccionRepository.countByOrdenProduccionId(80L)).thenReturn(0L);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "recalcularEstadoOrden", orden))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).isEqualTo("OP_SIN_CIERRES_PRODUCCION");
                });

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
        assertThat(orden.getFechaFin()).isNull();
    }

    @Test
    @DisplayName("recalcularEstadoOrden exige cierres incluso cuando hay producción acumulada")
    void recalcularEstadoOrden_rechazaSinCierresConProduccion() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(81L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProgramada(new BigDecimal("200"));
        orden.setCantidadProducidaAcumulada(new BigDecimal("50"));
        orden.setTipoCierre(TipoCierre.TOTAL);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(6L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(81L))
                .thenReturn(List.of(etapa));
        when(cierreProduccionRepository.countByOrdenProduccionId(81L)).thenReturn(0L);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "recalcularEstadoOrden", orden))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).isEqualTo("OP_SIN_CIERRES_PRODUCCION");
                });

        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
    }

    @Test
    @DisplayName("registrarCierre ejecuta consumo automático antes de cerrar la OP")
    void registrarCierre_disparaConsumo() {
        OrdenProduccion orden = crearOrdenBase(250L, new BigDecimal("50"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("50"))
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(250L, dto);

        verify(movimientoInventarioService).consumirInsumosPorOrden(250L, 1L, usuarioBasico().getId());
        verify(movimientoInventarioService).registrarMovimiento(any());
    }

    @Test
    @DisplayName("registrarCierre no re-ejecuta consumo cuando la orden ya está cerrada")
    void registrarCierre_idempotenciaOrdenFinalizada() {
        OrdenProduccion orden = crearOrdenBase(255L, new BigDecimal("50"), BigDecimal.ZERO, EstadoProduccion.FINALIZADA);
        when(ordenProduccionRepository.findByIdForUpdate(255L)).thenReturn(Optional.of(orden));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("5"))
                .tipo(TipoCierre.TOTAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(255L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("reason")
                .isEqualTo("OP_YA_FINALIZADA");

        verify(movimientoInventarioService, never()).consumirInsumosPorOrden(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("registrarCierre parcial no puede completar la cantidad programada")
    void registrarCierre_parcialNoCompletaProgramada() {
        OrdenProduccion orden = crearOrdenBase(310L, new BigDecimal("30000"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("30000"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(310L, dto))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(ex -> {
                    ErrorResponseException error = (ErrorResponseException) ex;
                    ProblemDetail body = error.getBody();
                    assertThat(body.getProperties().get("code")).isEqualTo("CIERRE_PARCIAL_SUPERA_PROGRAMADA");
                });
    }

    @Test
    @DisplayName("registrarCierre parcial no puede completar programada con el restante exacto")
    void registrarCierre_parcialNoCompletaConRestante() {
        OrdenProduccion orden = crearOrdenBase(311L, new BigDecimal("30000"), new BigDecimal("29999"), EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("1"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(311L, dto))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(ex -> {
                    ErrorResponseException error = (ErrorResponseException) ex;
                    ProblemDetail body = error.getBody();
                    assertThat(body.getProperties().get("code")).isEqualTo("CIERRE_PARCIAL_SUPERA_PROGRAMADA");
                });
    }

    @Test
    @DisplayName("registrarCierre parcial permite quedarse debajo de la programada")
    void registrarCierre_parcialPermiteAcumularMenor() {
        OrdenProduccion orden = crearOrdenBase(312L, new BigDecimal("30000"), new BigDecimal("10000"), EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("19999"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        OrdenProduccion resultado = service.registrarCierre(312L, dto);

        assertThat(resultado.getCantidadProducidaAcumulada()).isEqualByComparingTo(new BigDecimal("29999.00"));
        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
    }

    @Test
    @DisplayName("registrarCierre total debe cerrar restante exacto")
    void registrarCierre_totalDebeCerrarExacto() {
        OrdenProduccion orden = crearOrdenBase(313L, new BigDecimal("30000"), new BigDecimal("29999"), EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("1"))
                .tipo(TipoCierre.TOTAL)
                .build();

        OrdenProduccion resultado = service.registrarCierre(313L, dto);

        assertThat(resultado.getCantidadProducidaAcumulada()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.FINALIZADA);
    }

    @Test
    @DisplayName("registrarCierre total rechaza cantidad que no completa la programada")
    void registrarCierre_totalNoCoincideProgramada() {
        OrdenProduccion orden = crearOrdenBase(314L, new BigDecimal("30000"), new BigDecimal("20000"), EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("5000"))
                .tipo(TipoCierre.TOTAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(314L, dto))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(ex -> {
                    ErrorResponseException error = (ErrorResponseException) ex;
                    ProblemDetail body = error.getBody();
                    assertThat(body.getProperties().get("code")).isEqualTo("CIERRE_TOTAL_NO_COINCIDE_PROGRAMADA");
                });
    }

    @Test
    @DisplayName("registrarCierre parcial mantiene EN_PROCESO aun con etapas finalizadas")
    void registrarCierre_parcialNoCierraOrden() {
        OrdenProduccion orden = crearOrdenBase(300L, new BigDecimal("100"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("80"))
                .tipo(TipoCierre.PARCIAL)
                .cerradaIncompleta(false)
                .build();

        OrdenProduccion resultado = service.registrarCierre(300L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
        assertThat(resultado.getTipoCierre()).isNull();
        assertThat(resultado.getCantidadProducidaAcumulada()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("registrarCierre parcial devuelve delta de insumo y mantiene consumo neto real")
    void registrarCierre_parcialDevuelveDeltaInsumo() {
        OrdenProduccion orden = crearOrdenBase(277L, new BigDecimal("30000"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Producto insumo = new Producto();
        insumo.setId(30);
        insumo.setNombre("ME0460");
        UnidadMedida um = new UnidadMedida();
        um.setNombre("UND");
        um.setSimbolo("UND");
        insumo.setUnidadMedida(um);

        DetalleFormula det = new DetalleFormula();
        det.setInsumo(insumo);
        det.setCantidadNecesaria(BigDecimal.ONE);
        det.setUnidadMedida(um);

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(det));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        LoteProducto lote = new LoteProducto();
        lote.setId(2249L);
        lote.setCodigoLote("2249");
        when(loteProductoRepository.findById(2249L)).thenReturn(Optional.of(lote));

        MovimientoInventario consumo = new MovimientoInventario();
        consumo.setProducto(insumo);
        consumo.setLote(lote);
        consumo.setCantidad(new BigDecimal("30000"));
        consumo.setFechaIngreso(LocalDateTime.now().minusMinutes(5));

        MovimientoInventario traslado = new MovimientoInventario();
        traslado.setProducto(insumo);
        traslado.setLote(lote);
        Almacen origen = new Almacen();
        origen.setId(5);
        traslado.setAlmacenOrigen(origen);

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(consumo)));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(traslado)));
        when(loteProductoRepository.findByCodigoLoteAndProductoIdAndAlmacenId("2249", 30, 5))
                .thenReturn(Optional.of(new LoteProducto()));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("29900"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);

        service.registrarCierre(277L, dto);

        verify(movimientoInventarioService, atLeast(1)).registrarMovimiento(captor.capture());
        List<MovimientoInventarioDTO> movimientos = captor.getAllValues();
        List<MovimientoInventarioDTO> devoluciones = movimientos.stream()
                .filter(m -> m.tipoMovimiento() == TipoMovimiento.DEVOLUCION)
                .toList();

        assertThat(devoluciones).singleElement().satisfies(dev -> {
            assertThat(dev.productoId()).isEqualTo(30);
            assertThat(dev.loteProductoId()).isEqualTo(2249L);
            assertThat(dev.cantidad()).isEqualByComparingTo(new BigDecimal("100.000000"));
            assertThat(dev.almacenOrigenId()).isEqualTo(6);
            assertThat(dev.almacenDestinoId()).isEqualTo(5);
            assertThat(dev.ordenProduccionId()).isEqualTo(277L);
            assertThat(dev.clasificacionMovimientoInventario())
                    .isEqualTo(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION);
        });

        BigDecimal totalSalida = Optional.ofNullable(consumo.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal totalDevuelto = devoluciones.stream()
                .map(MovimientoInventarioDTO::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSalida.subtract(totalDevuelto))
                .isEqualByComparingTo(new BigDecimal("29900.000000"));

        verify(loteProductoRepository, never()).save(argThat(lp -> lp != null && "2249".equals(lp.getCodigoLote())));
    }

    @Test
    @DisplayName("registrarCierre total incompleto sin regularización es rechazado")
    void registrarCierre_totalIncompletoSinRegularizacionEsRechazado() {
        OrdenProduccion orden = crearOrdenBase(301L, new BigDecimal("100"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("80"))
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(true)
                .confirmarCierreParcial(false)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(301L, dto))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(ex -> {
                    ErrorResponseException error = (ErrorResponseException) ex;
                    ProblemDetail body = error.getBody();
                    assertThat(body.getProperties().get("code")).isEqualTo("CIERRE_TOTAL_NO_COINCIDE_PROGRAMADA");
                });
    }

    @Test
    @DisplayName("registrarCierre total incompleto con regularización se permite")
    void registrarCierre_totalIncompletoConRegularizacionPermitido() {
        OrdenProduccion orden = crearOrdenBase(302L, new BigDecimal("100"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        when(regularizacionTrazabilidadRepository.existsByOrdenProduccionId(302L)).thenReturn(true);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("80"))
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(true)
                .confirmarCierreParcial(true)
                .build();

        OrdenProduccion resultado = service.registrarCierre(302L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.FINALIZADA);
        assertThat(resultado.getCantidadProducidaAcumulada()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("registrarCierre total con producción completa finaliza la orden")
    void registrarCierre_totalCompletoFinaliza() {
        OrdenProduccion orden = crearOrdenBase(303L, new BigDecimal("100"), new BigDecimal("50"), EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("50"))
                .tipo(TipoCierre.TOTAL)
                .build();

        OrdenProduccion resultado = service.registrarCierre(303L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.FINALIZADA);
        assertThat(resultado.getTipoCierre()).isEqualTo(TipoCierre.TOTAL);
    }


    @Test
    @DisplayName("registrarCierre total 29900 sin regularización responde regla 422")
    void registrarCierre_total29900SinRegularizacionRechazado() {
        OrdenProduccion orden = crearOrdenBase(330L, new BigDecimal("30000"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("29900"))
                .tipo(TipoCierre.TOTAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(330L, dto))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(ex -> {
                    ErrorResponseException error = (ErrorResponseException) ex;
                    ProblemDetail body = error.getBody();
                    assertThat(body.getProperties().get("code")).isEqualTo("CIERRE_TOTAL_NO_COINCIDE_PROGRAMADA");
                });
    }

    @Test
    @DisplayName("registrarCierre total crea salidas de consumo real desde el alistado")
    void registrarCierre_totalGeneraSalidas() {
        OrdenProduccion orden = crearOrdenBase(400L, new BigDecimal("2"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Producto insumo = new Producto();
        insumo.setId(910);
        UnidadMedida um = new UnidadMedida();
        um.setNombre("kg");
        insumo.setUnidadMedida(um);
        DetalleFormula det = new DetalleFormula();
        det.setInsumo(insumo);
        det.setCantidadNecesaria(new BigDecimal("1.5"));
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(det));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        LoteProducto lote = new LoteProducto();
        lote.setId(77L);
        MovimientoInventario alistado = new MovimientoInventario();
        alistado.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        alistado.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        alistado.setProducto(insumo);
        alistado.setLote(lote);
        alistado.setCantidad(new BigDecimal("3.0"));
        Almacen preBodega = new Almacen();
        preBodega.setId(6);
        alistado.setAlmacenDestino(preBodega);

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(alistado)));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(orden.getId()),
                eq(insumo.getId().longValue()),
                eq(TipoMovimiento.SALIDA),
                eq(11L)
        )).thenReturn(BigDecimal.ZERO);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("2"))
                .tipo(TipoCierre.TOTAL)
                .build();

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);

        service.registrarCierre(400L, dto);

        verify(movimientoInventarioService, atLeast(1)).registrarMovimiento(captor.capture());
        List<MovimientoInventarioDTO> enviados = captor.getAllValues();

        List<MovimientoInventarioDTO> salidas = enviados.stream()
                .filter(m -> m.tipoMovimiento() == TipoMovimiento.SALIDA)
                .toList();
        List<MovimientoInventarioDTO> entradas = enviados.stream()
                .filter(m -> m.tipoMovimiento() == TipoMovimiento.ENTRADA)
                .toList();

        assertThat(entradas).singleElement().satisfies(entrada -> {
            assertThat(entrada.productoId()).isEqualTo(orden.getProducto().getId());
            assertThat(entrada.cantidad()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(entrada.almacenDestinoId()).isEqualTo(30);
            assertThat(entrada.ordenProduccionId()).isEqualTo(orden.getId());
        });
        if (!salidas.isEmpty()) {
            assertThat(salidas).anySatisfy(consumo -> {
                assertThat(consumo.clasificacionMovimientoInventario())
                        .isEqualTo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
                assertThat(consumo.productoId()).isEqualTo(insumo.getId());
                assertThat(consumo.loteProductoId()).isEqualTo(77L);
                assertThat(consumo.tipoMovimientoDetalleId()).isEqualTo(11L);
                assertThat(consumo.motivoMovimientoId()).isEqualTo(11L);
                assertThat(consumo.almacenOrigenId()).isEqualTo(6);
                assertThat(consumo.ordenProduccionId()).isEqualTo(orden.getId());
                assertThat(consumo.ordenProduccionEtapaId()).isEqualTo(1L);
            });
            assertThat(salidas.stream()
                    .map(MovimientoInventarioDTO::cantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo(new BigDecimal("3.000000"));
        }
    }

    @Test
    @DisplayName("registrarCierre total no duplica salidas ya registradas")
    void registrarCierre_totalIdempotenteEnSalidas() {
        OrdenProduccion orden = crearOrdenBase(401L, new BigDecimal("2"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Producto insumo = new Producto();
        insumo.setId(911);
        UnidadMedida um = new UnidadMedida();
        um.setNombre("kg");
        insumo.setUnidadMedida(um);
        DetalleFormula det = new DetalleFormula();
        det.setInsumo(insumo);
        det.setCantidadNecesaria(new BigDecimal("1.5"));
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(det));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        LoteProducto lote = new LoteProducto();
        lote.setId(78L);
        MovimientoInventario alistado = new MovimientoInventario();
        alistado.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        alistado.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        alistado.setProducto(insumo);
        alistado.setLote(lote);
        alistado.setCantidad(new BigDecimal("3.0"));
        Almacen preBodega = new Almacen();
        preBodega.setId(6);
        alistado.setAlmacenDestino(preBodega);

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(alistado)));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()),
                eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        AtomicInteger consultas = new AtomicInteger(0);
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(orden.getId()),
                eq(insumo.getId().longValue()),
                eq(TipoMovimiento.SALIDA),
                eq(11L)
        )).thenAnswer(inv -> consultas.getAndIncrement() == 0 ? BigDecimal.ZERO : new BigDecimal("3.0"));

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("2"))
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(401L, dto);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setCantidadProducida(BigDecimal.ZERO);
        orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);
        orden.setTipoCierre(null);
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(orden.getId()),
                eq(insumo.getId().longValue()),
                eq(TipoMovimiento.SALIDA),
                eq(11L)
        )).thenReturn(new BigDecimal("3.0"));
        service.registrarCierre(401L, dto);

        verify(movimientoInventarioService, atLeast(1)).registrarMovimiento(captor.capture());
        List<MovimientoInventarioDTO> enviados = captor.getAllValues();

        List<MovimientoInventarioDTO> salidas = enviados.stream()
                .filter(m -> m.tipoMovimiento() == TipoMovimiento.SALIDA)
                .toList();
        List<MovimientoInventarioDTO> entradas = enviados.stream()
                .filter(m -> m.tipoMovimiento() == TipoMovimiento.ENTRADA)
                .toList();

        assertThat(entradas).hasSize(2);
        assertThat(entradas).allSatisfy(entrada -> {
            assertThat(entrada.productoId()).isEqualTo(orden.getProducto().getId());
            assertThat(entrada.cantidad()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(entrada.almacenDestinoId()).isEqualTo(30);
            assertThat(entrada.ordenProduccionId()).isEqualTo(orden.getId());
        });

        assertThat(salidas).hasSizeLessThanOrEqualTo(1);
        if (!salidas.isEmpty()) {
            assertThat(salidas).singleElement().satisfies(consumo -> {
                assertThat(consumo.clasificacionMovimientoInventario())
                        .isEqualTo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
                assertThat(consumo.productoId()).isEqualTo(insumo.getId());
                assertThat(consumo.loteProductoId()).isEqualTo(78L);
                assertThat(consumo.cantidad()).isEqualByComparingTo(new BigDecimal("3.000000"));
                assertThat(consumo.tipoMovimientoDetalleId()).isEqualTo(11L);
                assertThat(consumo.motivoMovimientoId()).isEqualTo(11L);
                assertThat(consumo.almacenOrigenId()).isEqualTo(6);
                assertThat(consumo.ordenProduccionId()).isEqualTo(orden.getId());
                assertThat(consumo.ordenProduccionEtapaId()).isEqualTo(1L);
            });
        }
    }

    @Test
    @DisplayName("registrarCierre total usa la última etapa finalizada cuando no hay activa")
    void registrarCierre_totalSinEtapaActiva() {
        OrdenProduccion orden = crearOrdenBase(350L, new BigDecimal("40"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        LocalDateTime fin = LocalDateTime.now().minusHours(1);
        EtapaProduccion etapaFinalizada = EtapaProduccion.builder()
                .id(5L)
                .estado(EstadoEtapa.FINALIZADA)
                .fechaInicio(fin.minusHours(1))
                .fechaFin(fin)
                .build();
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId()))
                .thenReturn(List.of(etapaFinalizada));
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("40"))
                .tipo(TipoCierre.TOTAL)
                .confirmarCierreParcial(true)
                .build();

        assertThatCode(() -> service.registrarCierre(350L, dto)).doesNotThrowAnyException();

        verify(movimientoInventarioService).consumirInsumosPorOrden(350L, 5L, usuarioBasico().getId());
    }

    @Test
    @DisplayName("registrarCierre registra consumo real antes de consultar insumos")
    void registrarCierre_actualizaConsumido() {
        OrdenProduccion orden = crearOrdenBase(360L, new BigDecimal("4"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        Producto insumo = new Producto();
        insumo.setId(600);
        insumo.setNombre("Insumo prueba");
        UnidadMedida um = new UnidadMedida();
        um.setNombre("kg");
        insumo.setUnidadMedida(um);
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("2"));
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        AtomicBoolean consumoEjecutado = new AtomicBoolean(false);
        doAnswer(inv -> {
            consumoEjecutado.set(true);
            return null;
        }).when(movimientoInventarioService).consumirInsumosPorOrden(eq(360L), anyLong(), anyLong());
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(360L),
                eq(600L),
                eq(TipoMovimiento.SALIDA),
                eq(11L)
        )).thenAnswer(inv -> consumoEjecutado.get() ? new BigDecimal("6") : BigDecimal.ZERO);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("3"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        service.registrarCierre(360L, dto);
        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> insumos = service.listarInsumos(360L);

        assertThat(insumos).hasSize(1);
        assertThat(insumos.get(0).getCantidadConsumida()).isEqualByComparingTo(new BigDecimal("6"));
    }

    @Test
    @DisplayName("registrarCierre es idempotente al registrar consumos de insumos")
    void registrarCierre_idempotenciaConsumos() {
        OrdenProduccion orden = crearOrdenBase(370L, new BigDecimal("12"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger consumos = new AtomicInteger(0);
        doAnswer(inv -> {
            consumos.incrementAndGet();
            return null;
        }).when(movimientoInventarioService).consumirInsumosPorOrden(eq(370L), anyLong(), anyLong());
        AtomicInteger consultasConsumo = new AtomicInteger(0);
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(
                eq(370L),
                anyLong(),
                eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION)
        )).thenAnswer(inv -> consultasConsumo.getAndIncrement() == 0 ? List.of() : List.of(new MovimientoInventario()));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("5"))
                .tipo(TipoCierre.PARCIAL)
                .build();

        service.registrarCierre(370L, dto);
        service.registrarCierre(370L, dto);

        assertThat(consumos.get()).isEqualTo(1);
    }


    @Test
    @DisplayName("registrarCierre parcial y luego total en mismo lote crea dos entradas y finaliza")
    void registrarCierre_parcial_y_luego_total_mismo_lote() {
        OrdenProduccion orden = crearOrdenBase(500L, new BigDecimal("10800"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoteProducto lotePersistido = new LoteProducto();
        lotePersistido.setId(9001L);
        lotePersistido.setCodigoLote("L-9001");
        lotePersistido.setAlmacen(new Almacen(30));
        lotePersistido.setEstado(EstadoLote.DISPONIBLE);
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(500L, 500L))
                .thenReturn(Optional.empty(), Optional.of(lotePersistido));
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            if (lote.getId() == null) {
                lote.setId(9001L);
            }
            return lote;
        });

        CierreProduccionRequestDTO cierreParcial = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("10000"))
                .tipo(TipoCierre.PARCIAL)
                .build();
        CierreProduccionRequestDTO cierreTotal = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("800"))
                .tipo(TipoCierre.TOTAL)
                .build();

        service.registrarCierre(500L, cierreParcial);
        OrdenProduccion resultado = service.registrarCierre(500L, cierreTotal);

        ArgumentCaptor<MovimientoInventarioDTO> movimientosCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, times(2)).registrarMovimiento(movimientosCaptor.capture());
        List<MovimientoInventarioDTO> movimientos = movimientosCaptor.getAllValues();

        assertThat(movimientos).hasSize(2);
        assertThat(movimientos.get(0).cantidad()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(movimientos.get(1).cantidad()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(movimientos.get(0).tipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movimientos.get(1).tipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(resultado.getCantidadProducidaAcumulada()).isEqualByComparingTo(new BigDecimal("10800.00"));
        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.FINALIZADA);
    }

    @Test
    @DisplayName("registrarCierre reintento tras total con OP finalizada no crea más movimientos")
    void registrarCierre_reintentoPostTotal_ordenFinalizada() {
        OrdenProduccion orden = crearOrdenBase(501L, new BigDecimal("10800"), new BigDecimal("10800"), EstadoProduccion.FINALIZADA);
        when(ordenProduccionRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(orden));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("800"))
                .tipo(TipoCierre.TOTAL)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(501L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("reason")
                .isEqualTo("OP_YA_FINALIZADA");

        verify(cierreProduccionRepository, never()).save(any());
        verify(movimientoInventarioService, never()).registrarMovimiento(any());
        verify(movimientoInventarioService, never()).consumirInsumosPorOrden(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("registrarCierre definitivo rechaza cierre sin producción acumulada")
    void registrarCierre_definitivoSinProduccion() {
        OrdenProduccion orden = crearOrdenBase(304L, new BigDecimal("100"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 0L);

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("0.00"))
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(true)
                .confirmarCierreParcial(true)
                .build();

        assertThatThrownBy(() -> service.registrarCierre(304L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).isEqualTo("CANTIDAD_INVALIDA"));
    }

    @Test
    @DisplayName("listarInsumos refleja consumido en cero con reservas activas")
    void listarInsumos_conReservasActivas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(200L);
        orden.setCantidadProgramada(new BigDecimal("5"));
        Producto producto = new Producto();
        producto.setId(10);
        orden.setProducto(producto);

        Producto insumo = new Producto();
        insumo.setId(300);
        insumo.setNombre("Insumo A");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("kg");
        insumo.setUnidadMedida(unidad);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("2"));

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));

        when(ordenProduccionRepository.findById(200L)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                200L,
                300L,
                TipoMovimiento.SALIDA,
                11L))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacionSinEtapa(
                200L,
                300L,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                TipoMovimiento.TRANSFERENCIA))
                .thenReturn(new BigDecimal("1.25"));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacionSinEtapa(
                200L,
                300L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                TipoMovimiento.SALIDA))
                .thenReturn(BigDecimal.ZERO);

        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> lista = service.listarInsumos(200L);

        assertThat(lista).hasSize(1);
        com.willyes.clemenintegra.produccion.dto.InsumoOPDTO dto = lista.get(0);
        assertThat(dto.getCantidadRequerida()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(dto.getCantidadConsumida()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getFaltante()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(dto.getCantidadAlistada()).isEqualByComparingTo(new BigDecimal("1.25"));
    }

    @Test
    @DisplayName("listarInsumos omite insumos sin control de stock")
    void listarInsumos_omiteSinControlStock() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(202L);
        orden.setCantidadProgramada(new BigDecimal("5"));
        Producto producto = new Producto();
        producto.setId(10);
        orden.setProducto(producto);

        Producto insumo = new Producto();
        insumo.setId(302);
        insumo.setNombre("Insumo SM");
        insumo.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("2"));

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));

        when(ordenProduccionRepository.findById(202L)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> lista = service.listarInsumos(202L);

        assertThat(lista).isEmpty();
    }

    @Test
    @DisplayName("listarInsumos descuenta reservas consumidas")
    void listarInsumos_conReservasConsumidas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(201L);
        orden.setCantidadProgramada(new BigDecimal("5"));
        Producto producto = new Producto();
        producto.setId(10);
        orden.setProducto(producto);

        Producto insumo = new Producto();
        insumo.setId(301);
        insumo.setNombre("Insumo B");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("kg");
        insumo.setUnidadMedida(unidad);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(new BigDecimal("2"));

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));

        when(ordenProduccionRepository.findById(201L)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                201L,
                301L,
                TipoMovimiento.SALIDA,
                11L))
                .thenReturn(new BigDecimal("3"));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacionSinEtapa(
                201L,
                301L,
                ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION,
                TipoMovimiento.TRANSFERENCIA))
                .thenReturn(new BigDecimal("2.5"));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacionSinEtapa(
                201L,
                301L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                TipoMovimiento.SALIDA))
                .thenReturn(BigDecimal.ZERO);

        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> lista = service.listarInsumos(201L);

        assertThat(lista).hasSize(1);
        com.willyes.clemenintegra.produccion.dto.InsumoOPDTO dto = lista.get(0);
        assertThat(dto.getCantidadRequerida()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(dto.getCantidadConsumida()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(dto.getFaltante()).isEqualByComparingTo(new BigDecimal("7"));
        assertThat(dto.getCantidadAlistada()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    @DisplayName("clonarEtapasParaOrden crea etapas cuando la OP no tiene etapas previas")
    void clonarEtapasParaOrden_creaEtapas() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(10L);
        EtapaPlantilla etapa1 = EtapaPlantilla.builder().id(1L).nombre("Dispensado").secuencia(1).build();
        EtapaPlantilla etapa2 = EtapaPlantilla.builder().id(2L).nombre("Mezcla").secuencia(2).build();

        when(etapaProduccionRepository.existsByOrdenProduccionId(10L)).thenReturn(false);

        ArgumentCaptor<List<EtapaProduccion>> captor = ArgumentCaptor.forClass(List.class);
        service.clonarEtapasParaOrden(orden, List.of(etapa1, etapa2));

        verify(etapaProduccionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("clonarEtapasParaOrden es idempotente cuando ya existen etapas")
    void clonarEtapasParaOrden_idempotente() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(11L);
        EtapaPlantilla etapa = EtapaPlantilla.builder().id(1L).nombre("Envasado").secuencia(1).build();

        when(etapaProduccionRepository.existsByOrdenProduccionId(11L)).thenReturn(false, true);

        service.clonarEtapasParaOrden(orden, List.of(etapa));
        service.clonarEtapasParaOrden(orden, List.of(etapa));

        verify(etapaProduccionRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("clonarEtapasParaOrden traduce conflicto de integridad a 409")
    void clonarEtapasParaOrden_traduceConflict() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(12L);
        EtapaPlantilla etapa = EtapaPlantilla.builder().id(1L).nombre("Control").secuencia(1).build();

        when(etapaProduccionRepository.existsByOrdenProduccionId(12L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("dup"))
                .when(etapaProduccionRepository).saveAll(anyList());

        assertThatThrownBy(() -> service.clonarEtapasParaOrden(orden, List.of(etapa)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("ETAPAS_DUPLICADAS_OP");
                });
    }

    private OrdenProduccion crearOrdenBase(Long id, BigDecimal programada, BigDecimal producida, EstadoProduccion estado) {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(id);
        orden.setEstado(estado);
        orden.setCantidadProgramada(programada);
        orden.setCantidadProducida(producida);
        orden.setCantidadProducidaAcumulada(producida);
        orden.setFechaInicio(LocalDateTime.now().minusDays(2));
        orden.setProducto(crearProductoTerminado());
        return orden;
    }

    private Producto crearProductoTerminado() {
        Producto producto = new Producto();
        producto.setId(500);
        producto.setNombre("Producto terminado");
        UnidadMedida um = new UnidadMedida();
        um.setNombre("UND");
        um.setSimbolo("UND");
        producto.setUnidadMedida(um);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setCategoriaProducto(categoria);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        return producto;
    }

    private Usuario usuarioBasico() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombreCompleto("Tester");
        return usuario;
    }

    private void stubInfraCierre(OrdenProduccion orden, long cierresRegistrados) {
        when(ordenProduccionRepository.findByIdForUpdate(orden.getId())).thenReturn(Optional.of(orden));
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(
                orden.getId(), orden.getProducto().getId().longValue())).thenReturn(Optional.empty());
        FormulaProducto formulaVacia = new FormulaProducto();
        formulaVacia.setDetalles(List.of());
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(orden.getProducto().getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaVacia));

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(1L)
                .estado(EstadoEtapa.FINALIZADA)
                .fechaInicio(LocalDateTime.now().minusHours(5))
                .build();
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(orden.getId()))
                .thenReturn(List.of(etapa));

        VidaUtilProducto vidaUtil = new VidaUtilProducto();
        vidaUtil.setSemanasVigencia(4);
        when(vidaUtilProductoService.buscarPorProductoId(orden.getProducto().getId()))
                .thenReturn(Optional.of(vidaUtil));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);
        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(10L);
        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(10L);
        when(motivoMovimientoRepository.findById(10L)).thenReturn(Optional.of(motivoDev));

        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of());
        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), isNull(), isNull(), isNull(), eq(false), any()))
                .thenReturn(List.of());
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(11L);
        when(catalogResolver.getTipoDetalleSalidaProduccionId()).thenReturn(11L);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(null);
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(20L);
        MotivoMovimiento motivoEntrada = new MotivoMovimiento();
        motivoEntrada.setId(20L);
        when(motivoMovimientoRepository.findById(20L)).thenReturn(Optional.of(motivoEntrada));
        when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(21L);
        TipoMovimientoDetalle tipoEntrada = new TipoMovimientoDetalle();
        tipoEntrada.setId(21L);
        when(tipoMovimientoDetalleRepository.findById(21L)).thenReturn(Optional.of(tipoEntrada));
        when(movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(anyLong(), anyLong(), any()))
                .thenReturn(List.of());
        when(movimientoInventarioRepository
                .findFirstByOrdenProduccionIdAndTipoMovimientoAndClasificacionOrderByIdAsc(anyLong(), any(), any()))
                .thenReturn(Optional.empty());
        when(movimientoInventarioRepository
                .findFirstByOrdenProduccionIdAndLoteIdAndTipoMovimientoAndClasificacionOrderByIdAsc(anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        when(catalogResolver.getAlmacenPtId()).thenReturn(30L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(31L);
        Almacen almacenPt = new Almacen();
        almacenPt.setId(30);
        Almacen almacenCuarentena = new Almacen();
        almacenCuarentena.setId(31);
        when(almacenRepository.findById(30L)).thenReturn(Optional.of(almacenPt));
        when(almacenRepository.findById(31L)).thenReturn(Optional.of(almacenCuarentena));
        Almacen preBodega = new Almacen();
        preBodega.setId(6);
        when(almacenRepository.findById(6L)).thenReturn(Optional.of(preBodega));

        when(loteProductoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuarioBasico());
        when(cierreProduccionRepository.countByOrdenProduccionId(orden.getId())).thenReturn(cierresRegistrados);
        doNothing().when(reservaLoteService).liberarReservasPorOrden(anyLong());
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());
    }

    @Test
    @DisplayName("iniciarEtapa impide crear lote cuando no existe vida útil configurada para producto terminado")
    void iniciarEtapa_rechazaSinVidaUtil() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(8L);
        orden.setEstado(EstadoProduccion.CREADA);
        orden.setCodigoOrden("OP-008");

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);

        Producto producto = new Producto();
        producto.setId(25);
        producto.setCategoriaProducto(categoria);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        orden.setProducto(producto);

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(25L)
                .ordenProduccion(orden)
                .estado(EstadoEtapa.PENDIENTE)
                .secuencia(1)
                .build();

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .estado(EstadoSolicitudMovimiento.EJECUTADA)
                .ordenProduccion(orden)
                .build();

        when(ordenProduccionRepository.findById(8L)).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(25L)).thenReturn(Optional.of(etapa));
        when(solicitudMovimientoRepository.findByOrdenProduccionId(8L)).thenReturn(List.of(solicitud));
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(new Usuario());
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(new Almacen(1)));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(new Almacen(2)));
        when(vidaUtilProductoService.buscarPorProductoId(25)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarEtapa(8L, 25L))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("code")
                .isEqualTo(ApiErrorCode.VIDA_UTIL_NO_CONFIGURADA);
    }

    @Test
    @DisplayName("reservarInsumosParaOP incluye MP y ME para OP de producto semielaborado")
    void reservarInsumosParaOP_psIncluyeTodosLosInsumos() {
        doCallRealMethod().when(service).reservarInsumosParaOP(anyLong(), any());
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA");

        Producto productoPs = productoConCategoria(100, TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(500L);
        orden.setProducto(productoPs);
        orden.setCantidadProgramada(BigDecimal.TEN);

        when(ordenProduccionRepository.findById(500L)).thenReturn(Optional.of(orden));

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(crearFormulaPs());
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(100L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        stubInfraReserva();
        stubDisponibilidadGenerica();

        service.reservarInsumosParaOP(500L, null);

        ArgumentCaptor<SolicitudMovimientoRequestDTO> captor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService, times(6)).registrarSolicitud(captor.capture());

        List<Long> productosSolicitados = captor.getAllValues().stream()
                .map(SolicitudMovimientoRequestDTO::getProductoId)
                .toList();

        assertThat(productosSolicitados)
                .containsExactlyInAnyOrder(201L, 202L, 203L, 204L, 205L, 206L);
    }

    @Test
    @DisplayName("reservarInsumosParaOP en PT con PS incluye MP, PS y ME")
    void reservarInsumosParaOP_ptConPsIncluyeTodos() {
        doCallRealMethod().when(service).reservarInsumosParaOP(anyLong(), any());
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA");

        Producto productoPt = productoConCategoria(101, TipoCategoria.PRODUCTO_TERMINADO);
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(600L);
        orden.setProducto(productoPt);
        orden.setCantidadProgramada(BigDecimal.TEN);

        when(ordenProduccionRepository.findById(600L)).thenReturn(Optional.of(orden));

        DetalleFormula insumoPs = detalleFormula(301L, TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        DetalleFormula insumoMe = detalleFormula(302L, TipoCategoria.MATERIAL_EMPAQUE);
        DetalleFormula insumoMp = detalleFormula(303L, TipoCategoria.MATERIA_PRIMA);
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(insumoPs, insumoMe, insumoMp));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(101L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        stubInfraReserva();
        stubDisponibilidadGenerica();

        service.reservarInsumosParaOP(600L, null);

        ArgumentCaptor<SolicitudMovimientoRequestDTO> captor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService, times(3)).registrarSolicitud(captor.capture());

        List<Long> productosSolicitados = captor.getAllValues().stream()
                .map(SolicitudMovimientoRequestDTO::getProductoId)
                .toList();

        assertThat(productosSolicitados)
                .containsExactlyInAnyOrder(301L, 302L, 303L);
    }

    @Test
    @DisplayName("reservarInsumosParaOP conserva comportamiento para PT sin PS")
    void reservarInsumosParaOP_ptSinPsReservaTodos() {
        doCallRealMethod().when(service).reservarInsumosParaOP(anyLong(), any());
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE,AUTORIZADA");

        Producto productoPt = productoConCategoria(102, TipoCategoria.PRODUCTO_TERMINADO);
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(700L);
        orden.setProducto(productoPt);
        orden.setCantidadProgramada(BigDecimal.ONE);

        when(ordenProduccionRepository.findById(700L)).thenReturn(Optional.of(orden));

        DetalleFormula insumoMp = detalleFormula(401L, TipoCategoria.MATERIA_PRIMA);
        DetalleFormula insumoMe = detalleFormula(402L, TipoCategoria.MATERIAL_EMPAQUE);
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(insumoMp, insumoMe));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(102L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        stubInfraReserva();
        stubDisponibilidadGenerica();

        service.reservarInsumosParaOP(700L, null);

        ArgumentCaptor<SolicitudMovimientoRequestDTO> captor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService, times(2)).registrarSolicitud(captor.capture());

        List<Long> productosSolicitados = captor.getAllValues().stream()
                .map(SolicitudMovimientoRequestDTO::getProductoId)
                .toList();

        assertThat(productosSolicitados)
                .containsExactlyInAnyOrder(401L, 402L);
    }

    @Test
    @DisplayName("buscarPorId retorna fecha de vencimiento del lote PT cuando existe")
    void buscarPorId_conLotePtIncluyeFechaVencimiento() {
        Producto producto = new Producto();
        producto.setId(1);

        OrdenProduccion orden = OrdenProduccion.builder()
                .id(10L)
                .producto(producto)
                .estado(EstadoProduccion.CREADA)
                .build();

        LocalDateTime fechaVencimiento = LocalDateTime.now().plusMonths(6);
        LoteProducto lote = LoteProducto.builder()
                .id(5L)
                .fechaVencimiento(fechaVencimiento)
                .build();

        when(ordenProduccionRepository.findByIdWithProductoCategoria(10L)).thenReturn(Optional.of(orden));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(10L, 1L))
                .thenReturn(Optional.of(lote));

        Optional<OrdenProduccion> resultado = service.buscarPorId(10L);

        assertThat(resultado).isPresent();
        OrdenProduccionResponseDTO dto = ProduccionMapper.toResponse(resultado.get());
        assertThat(dto.fechaVencimientoLotePt).isEqualTo(fechaVencimiento);
    }

    @Test
    @DisplayName("buscarPorId deja fecha de vencimiento del lote PT en null cuando no hay lote")
    void buscarPorId_sinLotePtDevuelveFechaNull() {
        Producto producto = new Producto();
        producto.setId(2);

        OrdenProduccion orden = OrdenProduccion.builder()
                .id(20L)
                .producto(producto)
                .estado(EstadoProduccion.CREADA)
                .build();

        when(ordenProduccionRepository.findByIdWithProductoCategoria(20L)).thenReturn(Optional.of(orden));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(20L, 2L))
                .thenReturn(Optional.empty());

        Optional<OrdenProduccion> resultado = service.buscarPorId(20L);

        assertThat(resultado).isPresent();
        OrdenProduccionResponseDTO dto = ProduccionMapper.toResponse(resultado.get());
        assertThat(dto.fechaVencimientoLotePt).isNull();
    }

    private void stubInfraReserva() {
        Usuario usuario = usuarioBasico();
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(55L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));

        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(66L);
        when(tipoMovimientoDetalleRepository.findById(66L)).thenReturn(Optional.of(tipoDetalle));
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(66L);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(77L);

        when(solicitudMovimientoRepository.findWithDetalles(anyLong(), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of());

        AtomicLong secuencia = new AtomicLong(1);
        Map<Long, SolicitudMovimiento> solicitudes = new HashMap<>();

        when(solicitudMovimientoService.registrarSolicitud(any())).thenAnswer(invocation -> {
            SolicitudMovimientoRequestDTO req = invocation.getArgument(0);
            long id = secuencia.getAndIncrement();

            SolicitudMovimientoResponseDTO dto = new SolicitudMovimientoResponseDTO();
            dto.setId(id);

            SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                    .id(id)
                    .tipoMovimiento(req.getTipoMovimiento())
                    .cantidad(req.getCantidad())
                    .producto(crearProducto(req.getProductoId().intValue(), TipoCategoria.MATERIA_PRIMA))
                    .almacenDestino(new Almacen(Math.toIntExact(req.getAlmacenDestinoId())))
                    .detalles(new ArrayList<>())
                    .build();
            solicitudes.put(id, solicitud);
            return dto;
        });

        when(solicitudMovimientoRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(solicitudes.get(invocation.getArgument(0))));
        when(solicitudMovimientoRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(reservaLoteService).sincronizarReservasSolicitud(any());
    }

    @Test
    void listarConsumosPorEtapa_usaClasificacionPorDefecto() {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setId(55L);

        MovimientoInventarioResponseDTO respuesta = MovimientoInventarioResponseDTO.builder()
                .id(55L)
                .build();

        when(movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoAsc(
                        10L,
                        20L,
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(List.of(movimiento));
        when(movimientoInventarioMapper.safeToResponseDTO(movimiento)).thenReturn(respuesta);

        List<MovimientoInventarioResponseDTO> consumos = service.listarConsumosPorEtapa(10L, 20L, null);

        assertThat(consumos).containsExactly(respuesta);
    }

    @Test
    void listarMovimientosPorEtapa_filtraPorOrdenEtapaYClasificacion() {
        OrdenProduccion orden = OrdenProduccion.builder().id(10L).build();
        EtapaProduccion etapa = EtapaProduccion.builder().id(20L).ordenProduccion(orden).build();
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(etapaProduccionRepository.findById(etapa.getId())).thenReturn(Optional.of(etapa));

        MovimientoInventario movimiento = new MovimientoInventario();
        MovimientoInventarioResponseDTO dto = MovimientoInventarioResponseDTO.builder().id(77L).build();

        when(movimientoInventarioRepository
                .findByOrdenProduccionIdAndOrdenProduccionEtapaIdAndClasificacionOrderByFechaIngresoDesc(
                        orden.getId(),
                        etapa.getId(),
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(List.of(movimiento));
        when(movimientoInventarioMapper.safeToResponseDTO(movimiento)).thenReturn(dto);

        List<MovimientoInventarioResponseDTO> respuesta = service.listarMovimientosPorEtapa(
                orden.getId(),
                etapa.getId(),
                null
        );

        assertThat(respuesta).containsExactly(dto);
    }

    private void stubDisponibilidadGenerica() {
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(any()))
                .thenReturn(List.of(90L));

        when(disponibilidadInsumoService.calcularDisponibilidad(anyLong(), any(BigDecimal.class), anyList(), anyBoolean()))
                .thenAnswer(invocation -> distribucionGenerica(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        90L));

        when(disponibilidadInsumoService.calcularDisponibilidad(anyLong(), any(BigDecimal.class), anyList(), anyBoolean(), any(), anyBoolean()))
                .thenAnswer(invocation -> distribucionGenerica(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        90L));
    }

    private DistribucionFefoResult distribucionGenerica(Long productoId, BigDecimal requerida, Long almacenId) {
        BigDecimal requeridaEscala = requerida.setScale(6, RoundingMode.HALF_UP);
        DistribucionFefoDetalle detalle = DistribucionFefoDetalle.builder()
                .loteProductoId(productoId * 10)
                .almacenId(almacenId)
                .cantidadCalculo(requeridaEscala.setScale(8, RoundingMode.HALF_UP))
                .cantidadReserva(requeridaEscala)
                .build();

        return DistribucionFefoResult.builder()
                .productoInsumoId(productoId)
                .requerido(requeridaEscala)
                .stockFisicoTotal(requeridaEscala)
                .stockReservadoTotal(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                .stockLibreTotal(requeridaEscala)
                .faltante(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP))
                .suficiente(true)
                .detalles(List.of(detalle))
                .build();
    }

    private List<DetalleFormula> crearFormulaPs() {
        DetalleFormula mp1 = detalleFormula(201L, TipoCategoria.MATERIA_PRIMA);
        DetalleFormula mp2 = detalleFormula(202L, TipoCategoria.MATERIA_PRIMA);
        DetalleFormula me1 = detalleFormula(203L, TipoCategoria.MATERIAL_EMPAQUE);
        DetalleFormula me2 = detalleFormula(204L, TipoCategoria.MATERIAL_EMPAQUE);
        DetalleFormula me3 = detalleFormula(205L, TipoCategoria.MATERIAL_EMPAQUE);
        DetalleFormula me4 = detalleFormula(206L, TipoCategoria.MATERIAL_EMPAQUE);
        return List.of(mp1, mp2, me1, me2, me3, me4);
    }

    private DetalleFormula detalleFormula(Long productoId, TipoCategoria categoria) {
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(crearProducto(productoId.intValue(), categoria));
        detalle.setCantidadNecesaria(BigDecimal.ONE);
        return detalle;
    }

    private Producto productoConCategoria(int id, TipoCategoria tipoCategoria) {
        return crearProducto(id, tipoCategoria);
    }

    private Producto crearProducto(int id, TipoCategoria tipoCategoria) {
        Producto producto = new Producto();
        producto.setId(id);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipoCategoria);
        producto.setCategoriaProducto(categoria);
        producto.setModoControlInventario(ModoControlInventario.CONTROL_STOCK);
        return producto;
    }

    private DistribucionFefoResult crearDistribucionJarabe() {
        return DistribucionFefoResult.builder()
                .productoInsumoId(38L)
                .requerido(new BigDecimal("139650.000000"))
                .stockFisicoTotal(new BigDecimal("204150.000000"))
                .stockReservadoTotal(BigDecimal.ZERO)
                .stockLibreTotal(new BigDecimal("199150.000000"))
                .faltante(BigDecimal.ZERO.setScale(6))
                .suficiente(true)
                .detalles(List.of(
                        detalleFefo(117L, "L-120925-3", "10000.000000", "10000"),
                        detalleFefo(92L, "L-110925-3", "30000.000000", "30000"),
                        detalleFefo(118L, "L-120925-2", "10000.000000", "10000"),
                        detalleFefo(85L, "L-160925-2", "25000.000000", "25000"),
                        detalleFefo(103L, "L-180925-8", "59500.000000", "59500"),
                        detalleFefo(119L, "L-050825-3", "5000.000000", "5000"),
                        detalleFefo(156L, "L-151125-2", "150.000000", "59650")
                ))
                .build();
    }

    private DistribucionFefoDetalle detalleFefo(Long loteId, String codigo, String cantidad, String disponible) {
        BigDecimal reserva = new BigDecimal(cantidad).setScale(6, RoundingMode.HALF_UP);
        return DistribucionFefoDetalle.builder()
                .loteProductoId(loteId)
                .codigoLote(codigo)
                .almacenId(5L)
                .cantidadCalculo(reserva.setScale(8, RoundingMode.HALF_UP))
                .cantidadReserva(reserva)
                .disponible(new BigDecimal(disponible))
                .estado("DISPONIBLE")
                .build();
    }
}
