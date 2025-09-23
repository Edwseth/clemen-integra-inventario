package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.service.UnidadConversionService;
import com.willyes.clemenintegra.produccion.dto.InsumoFaltanteDTO;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceImplTest {

    @Mock
    private FormulaProductoRepository formulaProductoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SolicitudMovimientoService solicitudMovimientoService;
    @Mock
    private OrdenProduccionRepository repository;
    @Mock
    private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock
    private CierreProduccionRepository cierreProduccionRepository;
    @Mock
    private MovimientoInventarioService movimientoInventarioService;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private UnidadConversionService unidadConversionService;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;
    @Mock
    private EtapaPlantillaRepository etapaPlantillaRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock
    private InventoryCatalogResolver catalogResolver;
    @Mock
    private UmValidator umValidator;
    @Mock
    private VidaUtilProductoRepository vidaUtilProductoRepository;
    @Mock
    private ReservaLoteService reservaLoteService;

    @Spy
    @InjectMocks
    private OrdenProduccionServiceImpl service;

    @ParameterizedTest(name = "rechaza stock solo en pre-bodega para {0}")
    @MethodSource("categoriasInsumoOrigen")
    @SuppressWarnings("unchecked")
    void guardarConValidacionStock_rechazaCuandoSoloHayStockEnPreBodega(TipoCategoria tipoCategoria) {
        Long preBodegaId = 20L;
        Long almacenOrigenId = switch (tipoCategoria) {
            case MATERIA_PRIMA -> 10L;
            case MATERIAL_EMPAQUE -> 11L;
            case SUMINISTROS -> 12L;
            default -> throw new IllegalArgumentException("Tipo de categoría no soportado: " + tipoCategoria);
        };

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(preBodegaId);
        switch (tipoCategoria) {
            case MATERIA_PRIMA -> when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(almacenOrigenId);
            case MATERIAL_EMPAQUE -> when(catalogResolver.getAlmacenOrigenMaterialEmpaqueId()).thenReturn(almacenOrigenId);
            case SUMINISTROS -> when(catalogResolver.getAlmacenOrigenSuministrosId()).thenReturn(almacenOrigenId);
        }

        Producto productoFinal = new Producto();
        productoFinal.setId(1);
        productoFinal.setNombre("Producto terminado");

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(productoFinal);
        orden.setCantidadProgramada(new BigDecimal("5"));

        Producto insumo = new Producto();
        insumo.setId(2);
        insumo.setNombre("Insumo base");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        insumo.setUnidadMedida(unidad);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipoCategoria);
        insumo.setCategoriaProducto(categoria);

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(new BigDecimal("2"))
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(insumo));
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList()))
                .thenReturn(Map.of(insumo.getId().longValue(), BigDecimal.ZERO));
        when(stockQueryService.obtenerStockDisponible(anyList()))
                .thenReturn(Map.of(insumo.getId().longValue(), BigDecimal.ZERO));

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isFalse();
        assertThat(resultado.getInsumosFaltantes())
                .extracting(InsumoFaltanteDTO::getProductoId)
                .containsExactly(insumo.getId().longValue());

        ArgumentCaptor<List<Long>> productosCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Long>> almacenesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockQueryService).obtenerStockDisponible(productosCaptor.capture(), almacenesCaptor.capture());
        assertThat(productosCaptor.getValue()).containsExactly(insumo.getId().longValue());
        assertThat(almacenesCaptor.getValue()).containsExactly(almacenOrigenId);
        verify(stockQueryService).obtenerStockDisponible(eq(List.of(insumo.getId().longValue())));

        verifyNoInteractions(solicitudMovimientoService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardarConValidacionStock_consultaStockGlobalCuandoNoHayAlmacenConfigurado() {
        Long preBodegaId = 30L;
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(preBodegaId);

        Producto productoFinal = new Producto();
        productoFinal.setId(1);
        productoFinal.setNombre("Producto final");

        OrdenProduccion orden = new OrdenProduccion();
        orden.setProducto(productoFinal);
        orden.setCantidadProgramada(new BigDecimal("3"));

        Producto insumo = new Producto();
        insumo.setId(2);
        insumo.setNombre("Insumo sin origen");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        insumo.setUnidadMedida(unidad);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        insumo.setCategoriaProducto(categoria);

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(new BigDecimal("2"))
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(insumo));
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList()))
                .thenReturn(Map.of(insumo.getId().longValue(), BigDecimal.ONE));

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isFalse();
        assertThat(resultado.getInsumosFaltantes()).hasSize(1);
        assertThat(resultado.getInsumosFaltantes().get(0).getDisponible())
                .isEqualByComparingTo(BigDecimal.ONE);

        ArgumentCaptor<List<Long>> productosCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Long>> almacenesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockQueryService).obtenerStockDisponible(productosCaptor.capture(), almacenesCaptor.capture());
        assertThat(productosCaptor.getValue()).containsExactly(insumo.getId().longValue());
        assertThat(almacenesCaptor.getValue()).isEmpty();

        verifyNoInteractions(solicitudMovimientoService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardarConValidacionStock_validaConFallbackCuandoStockFueraDeAlmacen() {
        Long preBodegaId = 20L;
        Long almacenOrigenId = 10L;
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(preBodegaId);
        when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(almacenOrigenId);

        Producto productoFinal = new Producto();
        productoFinal.setId(1);
        productoFinal.setNombre("Producto final");

        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(100L);
        orden.setCodigoOrden("OP-EXISTENTE");
        orden.setProducto(productoFinal);
        orden.setCantidadProgramada(new BigDecimal("5"));
        orden.setCantidadProducida(BigDecimal.ZERO);
        orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);
        orden.setEstado(EstadoProduccion.CREADA);

        Usuario responsable = new Usuario();
        responsable.setId(50L);
        responsable.setNombreCompleto("Responsable OP");
        orden.setResponsable(responsable);

        Producto insumo = new Producto();
        insumo.setId(2);
        insumo.setNombre("Insumo externo");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setSimbolo("kg");
        insumo.setUnidadMedida(unidad);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        insumo.setCategoriaProducto(categoria);

        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(new BigDecimal("2"))
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));
        when(productoRepository.findAllById(any())).thenReturn(List.of(insumo));
        when(stockQueryService.obtenerStockDisponible(anyList(), anyList()))
                .thenReturn(Map.of(insumo.getId().longValue(), BigDecimal.ZERO));
        when(stockQueryService.obtenerStockDisponible(anyList()))
                .thenReturn(Map.of(insumo.getId().longValue(), new BigDecimal("15")));

        when(repository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(anyInt()))
                .thenReturn(List.of());
        doNothing().when(service).reservarInsumosParaOP(anyLong());

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(5L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION))
                .thenReturn(Optional.of(motivo));

        Long tipoDetalleId = 30L;
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleId);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(tipoDetalleId);
        when(tipoMovimientoDetalleRepository.findById(tipoDetalleId)).thenReturn(Optional.of(tipoDetalle));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(preBodegaId);
        when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().build());

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isTrue();
        assertThat(resultado.getInsumosFaltantes()).isNull();
        assertThat(resultado.getOrden()).isNotNull();

        verify(stockQueryService).obtenerStockDisponible(eq(List.of(insumo.getId().longValue())), eq(List.of(almacenOrigenId)));
        verify(stockQueryService).obtenerStockDisponible(eq(List.of(insumo.getId().longValue())));

        ArgumentCaptor<SolicitudMovimientoRequestDTO> solicitudCaptor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService).registrarSolicitud(solicitudCaptor.capture());
        assertThat(solicitudCaptor.getValue().getCantidad()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @ParameterizedTest(name = "reserva insumos desde almacén configurado para {0}")
    @MethodSource("categoriasInsumoOrigen")
    void reservarInsumosParaOP_reservaDesdeAlmacenConfigurado(TipoCategoria tipoCategoria) {
        Long ordenId = 7L;
        Long almacenOrigenId = switch (tipoCategoria) {
            case MATERIA_PRIMA -> 30L;
            case MATERIAL_EMPAQUE -> 31L;
            case SUMINISTROS -> 32L;
            default -> throw new IllegalArgumentException("Tipo de categoría no soportado: " + tipoCategoria);
        };

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(99L);
        switch (tipoCategoria) {
            case MATERIA_PRIMA -> when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(almacenOrigenId);
            case MATERIAL_EMPAQUE -> when(catalogResolver.getAlmacenOrigenMaterialEmpaqueId()).thenReturn(almacenOrigenId);
            case SUMINISTROS -> when(catalogResolver.getAlmacenOrigenSuministrosId()).thenReturn(almacenOrigenId);
        }

        Producto productoFinal = new Producto();
        productoFinal.setId(100);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(ordenId);
        orden.setProducto(productoFinal);
        orden.setCantidadProgramada(new BigDecimal("2"));

        Producto insumo = new Producto();
        insumo.setId(200);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipoCategoria);
        insumo.setCategoriaProducto(categoria);
        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.ONE)
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(repository.findById(ordenId)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(5L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));

        Long tipoDetalleSalidaId = 90L;
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleSalidaId);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(tipoDetalleSalidaId);
        when(tipoMovimientoDetalleRepository.findById(tipoDetalleSalidaId)).thenReturn(Optional.of(tipoDetalle));

        when(loteProductoRepository.findFefoDisponibles(insumo.getId().longValue(), Integer.MAX_VALUE))
                .thenReturn(List.of(
                        new TestLoteFefoDisponibleProjection(999L, new BigDecimal("10"), almacenOrigenId.intValue()),
                        new TestLoteFefoDisponibleProjection(998L, new BigDecimal("10"), 321)));

        SolicitudMovimientoResponseDTO respuesta = SolicitudMovimientoResponseDTO.builder()
                .id(400L)
                .build();
        when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(respuesta);

        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(respuesta.getId());
        solicitud.setDetalles(new ArrayList<>());
        when(solicitudMovimientoRepository.findById(respuesta.getId())).thenReturn(Optional.of(solicitud));
        when(solicitudMovimientoRepository.saveAndFlush(any(SolicitudMovimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reservarInsumosParaOP(ordenId);

        ArgumentCaptor<SolicitudMovimiento> solicitudCaptor = ArgumentCaptor.forClass(SolicitudMovimiento.class);
        verify(solicitudMovimientoRepository).saveAndFlush(solicitudCaptor.capture());

        SolicitudMovimiento guardada = solicitudCaptor.getValue();
        assertThat(guardada.getDetalles()).hasSize(1);
        assertThat(guardada.getDetalles().get(0).getAlmacenOrigen()).isNotNull();
        assertThat(guardada.getDetalles().get(0).getAlmacenOrigen().getId())
                .isEqualTo(almacenOrigenId);

        switch (tipoCategoria) {
            case MATERIA_PRIMA -> verify(catalogResolver).getAlmacenOrigenMateriaPrimaId();
            case MATERIAL_EMPAQUE -> verify(catalogResolver).getAlmacenOrigenMaterialEmpaqueId();
            case SUMINISTROS -> verify(catalogResolver).getAlmacenOrigenSuministrosId();
        }

        verify(reservaLoteService).sincronizarReservasSolicitud(solicitud);
    }

    @Test
    void reservarInsumosParaOP_noReservaLotesDePreBodega() {
        Long ordenId = 7L;
        Long almacenMaterialEmpaqueId = 10L;
        Long preBodegaId = 20L;
        when(catalogResolver.getAlmacenOrigenMaterialEmpaqueId()).thenReturn(almacenMaterialEmpaqueId);
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(preBodegaId);

        Producto productoFinal = new Producto();
        productoFinal.setId(100);

        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(ordenId);
        orden.setProducto(productoFinal);
        orden.setCantidadProgramada(new BigDecimal("3"));

        Producto insumo = new Producto();
        insumo.setId(200);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIAL_EMPAQUE);
        insumo.setCategoriaProducto(categoria);
        DetalleFormula detalle = DetalleFormula.builder()
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.ONE)
                .build();

        FormulaProducto formula = FormulaProducto.builder()
                .detalles(List.of(detalle))
                .build();

        when(repository.findById(ordenId)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoFinal.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(5L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));

        Long tipoDetalleSalidaId = 90L;
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(tipoDetalleSalidaId);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(tipoDetalleSalidaId);
        when(tipoMovimientoDetalleRepository.findById(tipoDetalleSalidaId)).thenReturn(Optional.of(tipoDetalle));

        when(loteProductoRepository.findFefoDisponibles(insumo.getId().longValue(), Integer.MAX_VALUE))
                .thenReturn(List.of(new TestLoteFefoDisponibleProjection(999L, new BigDecimal("3"), preBodegaId.intValue())));

        assertThatThrownBy(() -> service.reservarInsumosParaOP(ordenId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verifyNoInteractions(solicitudMovimientoService);
        verify(reservaLoteService, never()).sincronizarReservasSolicitud(any(SolicitudMovimiento.class));
    }

    private static Stream<TipoCategoria> categoriasInsumoOrigen() {
        return Stream.of(TipoCategoria.MATERIA_PRIMA, TipoCategoria.MATERIAL_EMPAQUE, TipoCategoria.SUMINISTROS);
    }

    private record TestLoteFefoDisponibleProjection(Long loteProductoId,
                                                    BigDecimal stockLote,
                                                    Integer almacenId) implements LoteFefoDisponibleProjection {
        @Override
        public String getCodigoLote() {
            return "LOT-" + loteProductoId;
        }

        @Override
        public LocalDateTime getFechaVencimiento() {
            return null;
        }

        @Override
        public String getNombreAlmacen() {
            return null;
        }
    }
}
