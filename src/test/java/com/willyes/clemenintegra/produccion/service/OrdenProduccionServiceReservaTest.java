package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.produccion.dto.CierreProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenProduccionServiceReservaTest {

    @Mock private FormulaProductoRepository formulaProductoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private StockQueryService stockQueryService;
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
    @Mock private com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private UmValidator umValidator;
    @Mock private VidaUtilProductoService vidaUtilProductoService;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;

    @InjectMocks
    private OrdenProduccionServiceImpl service;

    private OrdenProduccion orden;
    private Producto productoInsumo;

    @BeforeEach
    void setUp() {
        orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        Producto producto = new Producto();
        producto.setId(10);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        producto.setUnidadMedida(unidad);
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(false);
        producto.setRequiereAnalisisMicrobiologico(false);
        producto.recomputarTipoAnalisisDesdeBanderas();
        com.willyes.clemenintegra.inventario.model.CategoriaProducto categoriaPt = new com.willyes.clemenintegra.inventario.model.CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setCategoriaProducto(categoriaPt);
        producto.setNombre("Producto Final");
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("5.5"));

        productoInsumo = new Producto();
        productoInsumo.setId(50);
        productoInsumo.setCodigoSku("MP-COLRO");
        productoInsumo.setNombre("COLORANTE NATURAL ROJO");
        productoInsumo.setCategoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto());
        productoInsumo.getCategoriaProducto().setTipo(TipoCategoria.MATERIA_PRIMA);
        UnidadMedida unidadInsumo = new UnidadMedida();
        unidadInsumo.setNombre("MILILITRO");
        productoInsumo.setUnidadMedida(unidadInsumo);

        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenAnswer(invocation -> Optional.of(crearFormula()));
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), any(), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of());
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(99L);
        when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(5L);
        lenient().when(disponibilidadInsumoService.resolverAlmacenesPreferidos(productoInsumo))
                .thenReturn(List.of(5L));
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(11L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(33L);
        when(tipoMovimientoDetalleRepository.findById(anyLong())).thenReturn(Optional.of(tipoDetalle));
        lenient().when(solicitudMovimientoRepository.findById(100L)).thenReturn(Optional.of(crearSolicitudBase()));
        lenient().when(solicitudMovimientoRepository.saveAndFlush(any(SolicitudMovimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doNothing().when(reservaLoteService).sincronizarReservasSolicitud(any(SolicitudMovimiento.class));
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "EJECUTADA");
        ReflectionTestUtils.setField(service, "clasificacionEntradaPtConf", "ENTRADA_PRODUCTO_TERMINADO");
    }

    @Test
    @DisplayName("reservarInsumosParaOP genera detalles multi-lote con escala ajustada")
    void reservarInsumosParaOp_creaDetallesMultiLote() {
        when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(100L).build());
        DistribucionFefoResult resultado = DistribucionFefoResult.builder()
                .productoInsumoId(50L)
                .requerido(new BigDecimal("6.790123"))
                .stockFisicoTotal(new BigDecimal("13.123456"))
                .stockReservadoTotal(BigDecimal.ZERO)
                .stockLibreTotal(new BigDecimal("13.123456"))
                .faltante(BigDecimal.ZERO)
                .suficiente(true)
                .detalles(List.of(
                        DistribucionFefoDetalle.builder()
                                .loteProductoId(200L)
                                .almacenId(5L)
                                .cantidadCalculo(new BigDecimal("3.12345600"))
                                .cantidadReserva(new BigDecimal("3.123456"))
                                .disponible(new BigDecimal("3.123456"))
                                .estado("DISPONIBLE")
                                .build(),
                        DistribucionFefoDetalle.builder()
                                .loteProductoId(201L)
                                .almacenId(5L)
                                .cantidadCalculo(new BigDecimal("3.66666700"))
                                .cantidadReserva(new BigDecimal("3.666667"))
                                .disponible(new BigDecimal("10.000000"))
                                .estado("LIBERADO")
                                .build()
                ))
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(50L), any(BigDecimal.class), anyList(), eq(false)))
                .thenReturn(resultado);

        service.reservarInsumosParaOP(1L, null);

        ArgumentCaptor<SolicitudMovimientoRequestDTO> dtoCaptor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService).registrarSolicitud(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getCantidad()).isEqualByComparingTo(new BigDecimal("6.790123"));

        ArgumentCaptor<SolicitudMovimiento> solicitudCaptor = ArgumentCaptor.forClass(SolicitudMovimiento.class);
        verify(solicitudMovimientoRepository).saveAndFlush(solicitudCaptor.capture());
        List<SolicitudMovimientoDetalle> detalles = solicitudCaptor.getValue().getDetalles();
        assertThat(detalles).hasSize(2);
        BigDecimal total = detalles.stream()
                .map(SolicitudMovimientoDetalle::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(detalles.get(0).getCantidad().scale()).isEqualTo(6);
        assertThat(detalles.get(1).getCantidad().scale()).isEqualTo(6);
        assertThat(total).isEqualByComparingTo(new BigDecimal("6.790123"));
        verify(reservaLoteService).sincronizarReservasSolicitud(solicitudCaptor.getValue());
    }

    @Test
    @DisplayName("reservarInsumosParaOP usa el lote PS forzado cuando la fórmula lo requiere")
    void reservarInsumosParaOp_conPsUsaLoteForzado() {
        Producto insumoPs = new Producto();
        insumoPs.setId(200);
        insumoPs.setNombre("Base PS");
        insumoPs.setUnidadMedida(new UnidadMedida());
        com.willyes.clemenintegra.inventario.model.CategoriaProducto categoriaPs = new com.willyes.clemenintegra.inventario.model.CategoriaProducto();
        categoriaPs.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        insumoPs.setCategoriaProducto(categoriaPs);

        DetalleFormula detallePs = new DetalleFormula();
        detallePs.setInsumo(insumoPs);
        detallePs.setCantidadNecesaria(BigDecimal.ONE);

        FormulaProducto formulaPs = new FormulaProducto();
        formulaPs.setDetalles(List.of(detallePs));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPs));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(insumoPs)).thenReturn(List.of(5L));
        when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(100L).build());

        DistribucionFefoResult distribucion = DistribucionFefoResult.builder()
                .productoInsumoId(insumoPs.getId().longValue())
                .requerido(new BigDecimal("5.50000000"))
                .stockLibreTotal(new BigDecimal("6.000000"))
                .faltante(BigDecimal.ZERO)
                .suficiente(true)
                .detalles(List.of(DistribucionFefoDetalle.builder()
                        .loteProductoId(500L)
                        .almacenId(5L)
                        .cantidadCalculo(new BigDecimal("5.50000000"))
                        .cantidadReserva(new BigDecimal("5.500000"))
                        .disponible(new BigDecimal("6.000000"))
                        .estado(EstadoLote.LIBERADO.name())
                        .build()))
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(200L), any(BigDecimal.class), eq(List.of(5L)), eq(false), eq(500L), eq(true)))
                .thenReturn(distribucion);

        SolicitudMovimiento solicitudBase = new SolicitudMovimiento();
        solicitudBase.setId(100L);
        solicitudBase.setDetalles(new ArrayList<>());
        solicitudBase.setAlmacenDestino(new Almacen(Math.toIntExact(catalogResolver.getAlmacenPreBodegaProduccionId())));
        when(solicitudMovimientoRepository.findById(100L)).thenReturn(Optional.of(solicitudBase));

        service.reservarInsumosParaOP(1L, 500L);

        verify(disponibilidadInsumoService).calcularDisponibilidad(eq(200L), any(BigDecimal.class), eq(List.of(5L)), eq(false), eq(500L), eq(true));

        ArgumentCaptor<SolicitudMovimiento> solicitudCaptor = ArgumentCaptor.forClass(SolicitudMovimiento.class);
        verify(solicitudMovimientoRepository).saveAndFlush(solicitudCaptor.capture());
        List<SolicitudMovimientoDetalle> detallesGuardados = solicitudCaptor.getValue().getDetalles();
        assertThat(detallesGuardados).hasSize(1);
        assertThat(detallesGuardados.get(0).getLote().getId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("flujo FEFO detecta faltante por reservas y al reservar lanza STOCK_INSUFICIENTE")
    void flujoFefoDetectaFaltantePorReservas() {
        FormulaProducto formulaEscenario = new FormulaProducto();
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(productoInsumo);
        detalle.setCantidadNecesaria(new BigDecimal("0.5"));
        formulaEscenario.setDetalles(List.of(detalle));
        formulaEscenario.setProducto(orden.getProducto());

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEscenario));
        when(productoRepository.findAllById(any()))
                .thenReturn(List.of(productoInsumo));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(productoInsumo)).thenReturn(List.of(5L));
        when(productoRepository.findById(50L)).thenReturn(Optional.of(productoInsumo));

        DistribucionFefoResult preview = DistribucionFefoResult.builder()
                .productoInsumoId(50L)
                .requerido(new BigDecimal("350.000000"))
                .stockFisicoTotal(new BigDecimal("775.000000"))
                .stockReservadoTotal(new BigDecimal("472.500000"))
                .stockLibreTotal(new BigDecimal("302.500000"))
                .faltante(new BigDecimal("47.500000"))
                .suficiente(false)
                .almacenesPreferidos(List.of(5L))
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(50L), any(BigDecimal.class), eq(List.of(5L)), eq(true)))
                .thenReturn(preview);
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(50L), any(BigDecimal.class), eq(List.of(5L)), eq(false)))
                .thenReturn(preview);

        orden.setCantidadProgramada(new BigDecimal("700"));

        ResultadoValidacionOrdenDTO validacion = service.guardarConValidacionStock(orden);

        assertThat(validacion.isEsValida()).isFalse();
        assertThat(validacion.getUnidadesMaximasProducibles()).isEqualTo(605);
        assertThat(validacion.getInsumosFaltantes()).hasSize(1);
        assertThat(validacion.getInsumosFaltantes().get(0).getDisponible())
                .isEqualByComparingTo(new BigDecimal("302.500000"));

        assertThatThrownBy(() -> service.reservarInsumosParaOP(orden.getId(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessage("422 UNPROCESSABLE_ENTITY \"STOCK_INSUFICIENTE: insumo MP-COLRO - COLORANTE NATURAL ROJO, faltan 47.500000 MILILITRO\"");
    }

    @Test
    @DisplayName("reservarInsumosParaOP omite insumos configurados como SIN_CONTROL_STOCK")
    void reservarInsumosParaOp_omiteSinControlStock() {
        Producto insumoSinControl = new Producto();
        insumoSinControl.setId(120);
        insumoSinControl.setModoControlInventario(ModoControlInventario.SIN_CONTROL_STOCK);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("LITRO");
        insumoSinControl.setUnidadMedida(unidad);

        FormulaProducto formulaEscenario = new FormulaProducto();
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumoSinControl);
        detalle.setCantidadNecesaria(new BigDecimal("3"));
        formulaEscenario.setDetalles(List.of(detalle));
        formulaEscenario.setProducto(orden.getProducto());

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEscenario));

        service.reservarInsumosParaOP(1L, null);

        verify(solicitudMovimientoService, never()).registrarSolicitud(any(SolicitudMovimientoRequestDTO.class));
        verify(disponibilidadInsumoService, never()).calcularDisponibilidad(anyLong(), any(BigDecimal.class), anyList(), anyBoolean());
    }

    @Test
    @DisplayName("reservarInsumosParaOP no lanza error cuando Jarabe Base tiene stock FEFO suficiente")
    void reservarInsumosParaOp_jarabeBaseSinErrores() {
        Producto jarabe = new Producto();
        jarabe.setId(60);
        jarabe.setCodigoSku("MP-JARBA");
        jarabe.setNombre("JARABE BASE");
        jarabe.setCategoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto());
        jarabe.getCategoriaProducto().setTipo(TipoCategoria.MATERIA_PRIMA);
        UnidadMedida unidadMl = new UnidadMedida();
        unidadMl.setNombre("MILILITRO");
        jarabe.setUnidadMedida(unidadMl);

        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(jarabe);
        detalle.setCantidadNecesaria(new BigDecimal("199.5"));

        FormulaProducto formulaJarabe = new FormulaProducto();
        formulaJarabe.setProducto(orden.getProducto());
        formulaJarabe.setDetalles(List.of(detalle));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaJarabe));
        when(disponibilidadInsumoService.resolverAlmacenesPreferidos(jarabe)).thenReturn(List.of(5L));
        when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(100L).build());
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(60L), any(BigDecimal.class), eq(List.of(5L)), eq(false)))
                .thenReturn(crearDistribucionJarabeResult(60L));

        orden.setCantidadProgramada(new BigDecimal("700"));

        assertThatCode(() -> service.reservarInsumosParaOP(1L, null)).doesNotThrowAnyException();

        ArgumentCaptor<SolicitudMovimiento> solicitudCaptor = ArgumentCaptor.forClass(SolicitudMovimiento.class);
        verify(solicitudMovimientoRepository).saveAndFlush(solicitudCaptor.capture());
        List<SolicitudMovimientoDetalle> detalles = solicitudCaptor.getValue().getDetalles();
        assertThat(detalles).hasSize(7);
        BigDecimal total = detalles.stream()
                .map(SolicitudMovimientoDetalle::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(new BigDecimal("139650.000000"));
        verify(reservaLoteService).sincronizarReservasSolicitud(solicitudCaptor.getValue());
    }

    @Test
    @DisplayName("reservarInsumosParaOP falla con 422 cuando no hay lotes elegibles")
    void reservarInsumosParaOp_sinLotesLanzaError() {
        DistribucionFefoResult insuficiente = DistribucionFefoResult.builder()
                .productoInsumoId(50L)
                .requerido(new BigDecimal("6.790123"))
                .stockFisicoTotal(BigDecimal.ZERO)
                .stockReservadoTotal(BigDecimal.ZERO)
                .stockLibreTotal(BigDecimal.ZERO)
                .faltante(new BigDecimal("6.790123"))
                .suficiente(false)
                .detalles(List.of())
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(50L), any(BigDecimal.class), anyList(), eq(false)))
                .thenReturn(insuficiente);

        assertThatThrownBy(() -> service.reservarInsumosParaOP(1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("MP-COLRO")
                .hasMessageContaining("COLORANTE NATURAL ROJO")
                .hasMessageContaining("6.790123")
                .hasMessageContaining("MILILITRO")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(reservaLoteService, never()).sincronizarReservasSolicitud(any());
    }

    @Test
    @DisplayName("registrarCierre total sin pendientes libera reservas")
    void registrarCierre_totalSinPendientesLiberaReservas() {
        CierreProduccionRequestDTO dto = CierreProduccionRequestDTO.builder()
                .cantidad(new BigDecimal("1.25"))
                .tipo(TipoCierre.TOTAL)
                .confirmarCierreParcial(true)
                .build();

        when(umValidator.ajustar(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(umValidator.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        when(catalogResolver.decimals(any(UnidadMedida.class))).thenReturn(6);
        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(70L);
        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(70L);
        when(motivoMovimientoRepository.findById(70L)).thenReturn(Optional.of(motivoDev));
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), eq(null), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of());
        when(catalogResolver.getMotivoIdEntradaProductoTerminado()).thenReturn(80L);
        MotivoMovimiento motivoEntrada = new MotivoMovimiento();
        motivoEntrada.setId(80L);
        when(motivoMovimientoRepository.findById(80L)).thenReturn(Optional.of(motivoEntrada));
        when(catalogResolver.getTipoDetalleEntradaId()).thenReturn(90L);
        TipoMovimientoDetalle tipoDetalleEntrada = new TipoMovimientoDetalle();
        tipoDetalleEntrada.setId(90L);
        when(tipoMovimientoDetalleRepository.findById(90L)).thenReturn(Optional.of(tipoDetalleEntrada));
        EtapaProduccion etapa = new EtapaProduccion();
        etapa.setEstado(EstadoEtapa.FINALIZADA);
        etapa.setFechaInicio(LocalDateTime.now().minusDays(2));
        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(1L))
                .thenReturn(List.of(etapa));
        when(vidaUtilProductoService.buscarPorProductoId(10)).thenReturn(Optional.of(VidaUtilProducto.builder()
                .productoId(10)
                .semanasVigencia(4)
                .build()));
        when(ordenProduccionRepository.findTopByLoteProduccionStartingWithOrderByLoteProduccionDesc(any(String.class)))
                .thenReturn(Optional.empty());
        Almacen almacenPt = new Almacen();
        almacenPt.setId(1);
        Almacen almacenCuarentena = new Almacen();
        almacenCuarentena.setId(2);
        when(catalogResolver.getAlmacenPtId()).thenReturn(1L);
        when(catalogResolver.getAlmacenCuarentenaId()).thenReturn(2L);
        when(almacenRepository.findById(1L)).thenReturn(Optional.of(almacenPt));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(almacenCuarentena));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L)).thenReturn(Optional.empty());
        when(loteProductoRepository.findByCodigoLote(any())).thenReturn(Optional.empty());
        when(loteProductoRepository.save(any(LoteProducto.class))).thenAnswer(invocation -> {
            LoteProducto lote = invocation.getArgument(0);
            lote.setId(555L);
            return lote;
        });
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(cierreProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cierreProduccionRepository.countByOrdenProduccionId(1L)).thenReturn(1L);
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), any(), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of());
        when(movimientoInventarioService.registrarMovimiento(any())).thenReturn(new MovimientoInventarioResponseDTO());
        orden.setCodigoOrden("OP-001");

        service.registrarCierre(1L, dto);

        verify(reservaLoteService).liberarReservasPorOrden(1L);
    }

    private FormulaProducto crearFormula() {
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(productoInsumo);
        detalle.setCantidadNecesaria(new BigDecimal("1.23456789"));
        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(detalle));
        return formula;
    }

    private DistribucionFefoResult crearDistribucionJarabeResult(Long productoId) {
        return DistribucionFefoResult.builder()
                .productoInsumoId(productoId)
                .requerido(new BigDecimal("139650.000000"))
                .stockFisicoTotal(new BigDecimal("204150.000000"))
                .stockReservadoTotal(BigDecimal.ZERO)
                .stockLibreTotal(new BigDecimal("199150.000000"))
                .faltante(BigDecimal.ZERO.setScale(6))
                .suficiente(true)
                .detalles(List.of(
                        detalleJarabe(117L, "10000.000000", "10000"),
                        detalleJarabe(92L, "30000.000000", "30000"),
                        detalleJarabe(118L, "10000.000000", "10000"),
                        detalleJarabe(85L, "25000.000000", "25000"),
                        detalleJarabe(103L, "59500.000000", "59500"),
                        detalleJarabe(119L, "5000.000000", "5000"),
                        detalleJarabe(156L, "150.000000", "59650")
                ))
                .build();
    }

    private DistribucionFefoDetalle detalleJarabe(Long loteId, String cantidad, String disponible) {
        BigDecimal reserva = new BigDecimal(cantidad).setScale(6, RoundingMode.HALF_UP);
        return DistribucionFefoDetalle.builder()
                .loteProductoId(loteId)
                .almacenId(5L)
                .cantidadCalculo(reserva.setScale(8, RoundingMode.HALF_UP))
                .cantidadReserva(reserva)
                .disponible(new BigDecimal(disponible))
                .estado("DISPONIBLE")
                .build();
    }

    private SolicitudMovimiento crearSolicitudBase() {
        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(100L);
        solicitud.setDetalles(new ArrayList<>());
        Almacen destino = new Almacen();
        destino.setId(9);
        solicitud.setAlmacenDestino(destino);
        return solicitud;
    }

}
