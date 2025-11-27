package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.dto.OrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

    @Spy
    @InjectMocks
    private OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(service).reservarInsumosParaOP(anyLong(), any());
        lenient().when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> {
            OrdenProduccion op = invocation.getArgument(0);
            if (op.getId() == null) {
                op.setId(100L);
            }
            return op;
        });
        lenient().when(ordenProduccionRepository.countByCodigoOrdenStartingWith(any())).thenReturn(0L);
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

        OrdenProduccionRequestDTO dto = new OrdenProduccionRequestDTO();
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

        OrdenProduccionRequestDTO dto = new OrdenProduccionRequestDTO();
        dto.setProductoId(21L);
        dto.setResponsableId(6L);
        dto.setCantidadProgramada(new BigDecimal("10"));
        dto.setUnidadMedidaSimbolo("UND");
        dto.setEstado("CREADA");

        service.crearOrden(dto);

        verify(unidadConversionService).dividirNormalizado(
                new BigDecimal("10"),
                "UND",
                new BigDecimal("8"),
                "UND");
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
        assertThat(orden.getEstado()).isEqualTo(EstadoProduccion.EN_PROCESO);
        verify(ordenProduccionRepository).save(orden);
        verify(movimientoInventarioService).consumirInsumosPorOrden(1L, usuario.getId());
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
        producto.setTipoAnalisis(TipoAnalisisCalidad.NINGUNO);
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
        producto.setTipoAnalisis(TipoAnalisisCalidad.FISICO);
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
    @DisplayName("registrarCierre total incompleto exige confirmación antes de cerrar")
    void registrarCierre_totalIncompletoRequiereConfirmacion() {
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
                    assertThat(body.getProperties().get("code")).isEqualTo("OP_CIERRE_PARCIAL_REQUIERE_CONFIRMACION");
                });
    }

    @Test
    @DisplayName("registrarCierre total incompleto con confirmación marca CERRADA_INCOMPLETA")
    void registrarCierre_totalIncompletoConfirmado() {
        OrdenProduccion orden = crearOrdenBase(302L, new BigDecimal("100"), BigDecimal.ZERO, EstadoProduccion.EN_PROCESO);
        stubInfraCierre(orden, 1L);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("80"))
                .tipo(TipoCierre.TOTAL)
                .cerradaIncompleta(true)
                .confirmarCierreParcial(true)
                .build();

        OrdenProduccion resultado = service.registrarCierre(302L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProduccion.CERRADA_INCOMPLETA);
        assertThat(resultado.getTipoCierre()).isEqualTo(TipoCierre.PARCIAL);
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
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacion(
                200L,
                300L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                TipoMovimiento.SALIDA))
                .thenReturn(BigDecimal.ZERO);
        when(reservaLoteRepository.sumConsumidaByOrdenAndProducto(200L, 300L, EstadoReservaLote.CONSUMIDA))
                .thenReturn(BigDecimal.ZERO);

        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> lista = service.listarInsumos(200L);

        assertThat(lista).hasSize(1);
        com.willyes.clemenintegra.produccion.dto.InsumoOPDTO dto = lista.get(0);
        assertThat(dto.getCantidadRequerida()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(dto.getCantidadConsumida()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getFaltante()).isEqualByComparingTo(new BigDecimal("10"));
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
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoClasificacion(
                201L,
                301L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                TipoMovimiento.SALIDA))
                .thenReturn(BigDecimal.ONE);
        when(reservaLoteRepository.sumConsumidaByOrdenAndProducto(201L, 301L, EstadoReservaLote.CONSUMIDA))
                .thenReturn(new BigDecimal("3"));

        List<com.willyes.clemenintegra.produccion.dto.InsumoOPDTO> lista = service.listarInsumos(201L);

        assertThat(lista).hasSize(1);
        com.willyes.clemenintegra.produccion.dto.InsumoOPDTO dto = lista.get(0);
        assertThat(dto.getCantidadRequerida()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(dto.getCantidadConsumida()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(dto.getFaltante()).isEqualByComparingTo(new BigDecimal("7"));
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
        producto.setTipoAnalisis(TipoAnalisisCalidad.NINGUNO);
        return producto;
    }

    private Usuario usuarioBasico() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombreCompleto("Tester");
        return usuario;
    }

    private void stubInfraCierre(OrdenProduccion orden, long cierresRegistrados) {
        when(ordenProduccionRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(
                orden.getId(), orden.getProducto().getId().longValue())).thenReturn(Optional.empty());

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

        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(10L);
        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(10L);
        when(motivoMovimientoRepository.findById(10L)).thenReturn(Optional.of(motivoDev));

        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), any(), any(), any(), eq(false), any()))
                .thenReturn(List.of());
        when(solicitudMovimientoRepository.findWithDetalles(eq(orden.getId()), isNull(), isNull(), isNull(), eq(false), any()))
                .thenReturn(List.of());
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(11L);
        when(catalogResolver.getTipoDetalleTransferenciaId()).thenReturn(null);
        when(movimientoInventarioRepository.sumaPorSolicitudYTipo(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(20L);
        MotivoMovimiento motivoEntrada = new MotivoMovimiento();
        motivoEntrada.setId(20L);
        when(motivoMovimientoRepository.findById(20L)).thenReturn(Optional.of(motivoEntrada));
        when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(21L);
        TipoMovimientoDetalle tipoEntrada = new TipoMovimientoDetalle();
        tipoEntrada.setId(21L);
        when(tipoMovimientoDetalleRepository.findById(21L)).thenReturn(Optional.of(tipoEntrada));

        when(catalogResolver.getAlmacenPtId()).thenReturn(30L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(31L);
        Almacen almacenPt = new Almacen();
        almacenPt.setId(30);
        Almacen almacenCuarentena = new Almacen();
        almacenCuarentena.setId(31);
        when(almacenRepository.findById(30L)).thenReturn(Optional.of(almacenPt));
        when(almacenRepository.findById(31L)).thenReturn(Optional.of(almacenCuarentena));

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
        producto.setTipoAnalisis(TipoAnalisisCalidad.NINGUNO);
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
