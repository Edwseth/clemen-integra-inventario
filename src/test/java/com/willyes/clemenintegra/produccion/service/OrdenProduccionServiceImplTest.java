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
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    @Mock private VidaUtilProductoRepository vidaUtilProductoRepository;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private ReservaLoteRepository reservaLoteRepository;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;

    @Spy
    @InjectMocks
    private OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(service).reservarInsumosParaOP(anyLong());
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

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(1L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(50L))
                .thenReturn(List.of(etapa));

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

        EtapaProduccion etapa = EtapaProduccion.builder()
                .id(2L)
                .estado(EstadoEtapa.FINALIZADA)
                .build();

        when(etapaProduccionRepository.findByOrdenProduccionIdOrderBySecuenciaAsc(60L))
                .thenReturn(List.of(etapa));

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
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(11L);
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(11L);
        when(tipoMovimientoDetalleRepository.findById(11L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenYProducto(200L, 300L, 11L))
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
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(12L);
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(12L);
        when(tipoMovimientoDetalleRepository.findById(12L)).thenReturn(Optional.of(tipoDetalle));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenYProducto(201L, 301L, 12L))
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
