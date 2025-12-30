package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.model.enums.TipoCierre;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrdenProduccionServiceCierreTotalTest {

    private FormulaProductoRepository formulaProductoRepository;
    private ProductoRepository productoRepository;
    private UsuarioRepository usuarioRepository;
    private SolicitudMovimientoService solicitudMovimientoService;
    private OrdenProduccionRepository repository;
    private MotivoMovimientoRepository motivoMovimientoRepository;
    private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private CierreProduccionRepository cierreProduccionRepository;
    private MovimientoInventarioService movimientoInventarioService;
    private LoteProductoRepository loteProductoRepository;
    private AlmacenRepository almacenRepository;
    private UnidadConversionService unidadConversionService;
    private EtapaProduccionRepository etapaProduccionRepository;
    private EtapaPlantillaRepository etapaPlantillaRepository;
    private MovimientoInventarioRepository movimientoInventarioRepository;
    private MovimientoInventarioMapper movimientoInventarioMapper;
    private com.willyes.clemenintegra.shared.service.UsuarioService usuarioService;
    private SolicitudMovimientoRepository solicitudMovimientoRepository;
    private InventoryCatalogResolver catalogResolver;
    private UmValidator umValidator;
    private com.willyes.clemenintegra.calidad.service.VidaUtilProductoService vidaUtilProductoService;
    private ReservaLoteService reservaLoteService;
    private ReservaLoteRepository reservaLoteRepository;
    private DisponibilidadInsumoService disponibilidadInsumoService;

    private OrdenProduccionServiceImpl service;

    @BeforeEach
    void setUp() {
        formulaProductoRepository = mock(FormulaProductoRepository.class);
        productoRepository = mock(ProductoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        solicitudMovimientoService = mock(SolicitudMovimientoService.class);
        repository = mock(OrdenProduccionRepository.class);
        motivoMovimientoRepository = mock(MotivoMovimientoRepository.class);
        tipoMovimientoDetalleRepository = mock(TipoMovimientoDetalleRepository.class);
        cierreProduccionRepository = mock(CierreProduccionRepository.class);
        movimientoInventarioService = mock(MovimientoInventarioService.class);
        loteProductoRepository = mock(LoteProductoRepository.class);
        almacenRepository = mock(AlmacenRepository.class);
        unidadConversionService = mock(UnidadConversionService.class);
        etapaProduccionRepository = mock(EtapaProduccionRepository.class);
        etapaPlantillaRepository = mock(EtapaPlantillaRepository.class);
        movimientoInventarioRepository = mock(MovimientoInventarioRepository.class);
        movimientoInventarioMapper = mock(MovimientoInventarioMapper.class);
        usuarioService = mock(com.willyes.clemenintegra.shared.service.UsuarioService.class);
        solicitudMovimientoRepository = mock(SolicitudMovimientoRepository.class);
        catalogResolver = mock(InventoryCatalogResolver.class);
        umValidator = mock(UmValidator.class);
        vidaUtilProductoService = mock(com.willyes.clemenintegra.calidad.service.VidaUtilProductoService.class);
        reservaLoteService = mock(ReservaLoteService.class);
        reservaLoteRepository = mock(ReservaLoteRepository.class);
        disponibilidadInsumoService = mock(DisponibilidadInsumoService.class);

        service = new OrdenProduccionServiceImpl(
                formulaProductoRepository,
                productoRepository,
                usuarioRepository,
                solicitudMovimientoService,
                repository,
                motivoMovimientoRepository,
                tipoMovimientoDetalleRepository,
                cierreProduccionRepository,
                movimientoInventarioService,
                loteProductoRepository,
                almacenRepository,
                unidadConversionService,
                etapaProduccionRepository,
                etapaPlantillaRepository,
                movimientoInventarioRepository,
                movimientoInventarioMapper,
                usuarioService,
                solicitudMovimientoRepository,
                catalogResolver,
                umValidator,
                vidaUtilProductoService,
                reservaLoteService,
                reservaLoteRepository,
                disponibilidadInsumoService
        );
        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "ATENDIDO");
    }

    @Test
    void registrarConsumoRealPorCierreTotal_generaSalidaDesdePrebodega() {
        OrdenProduccion orden = ordenProduccion(new BigDecimal("5"));
        Usuario usuario = usuario(50L);
        EtapaProduccion etapa = new EtapaProduccion();
        etapa.setId(99L);
        List<EtapaProduccion> etapas = List.of(etapa);

        Producto insumo = producto(200);
        FormulaProducto formula = formula(insumo, new BigDecimal("2.0"));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(
                orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)).thenReturn(Optional.of(formula));

        when(catalogResolver.getTipoDetalleSalidaProduccionId()).thenReturn(10L);
        when(tipoMovimientoDetalleRepository.findById(10L)).thenReturn(Optional.of(tipoMovimientoDetalle(10L)));
        when(catalogResolver.getMotivoSalidaProduccionId()).thenReturn(11L);
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivoMovimiento(11L)));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);

        MovimientoInventario alistado = transferenciaPrebodega(insumo, 6, new BigDecimal("12"));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()), eq(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION), any()))
                .thenReturn(new PageImpl<>(List.of(alistado)));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()), eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(orden.getId()), eq(insumo.getId().longValue()), eq(TipoMovimiento.SALIDA), eq(10L)))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoInventarioService.registrarMovimiento(any()))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(999L).build());

        ReflectionTestUtils.invokeMethod(
                service,
                "registrarConsumoRealPorCierreTotal",
                orden,
                etapas,
                etapa,
                usuario
        );

        ArgumentCaptor<MovimientoInventarioDTO> dtoCaptor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(dtoCaptor.capture());
        MovimientoInventarioDTO dto = dtoCaptor.getValue();
        assertThat(dto.tipoMovimiento()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(dto.clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        assertThat(dto.almacenOrigenId()).isEqualTo(6);
        assertThat(dto.cantidad()).isEqualByComparingTo(new BigDecimal("10.0"));
    }

    @Test
    void registrarConsumoRealPorCierreTotal_idempotenteCuandoYaConsumido() {
        OrdenProduccion orden = ordenProduccion(new BigDecimal("3"));
        Usuario usuario = usuario(51L);
        Producto insumo = producto(201);
        FormulaProducto formula = formula(insumo, new BigDecimal("1.5"));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(
                orden.getProducto().getId().longValue(), EstadoFormula.APROBADA)).thenReturn(Optional.of(formula));

        when(catalogResolver.getTipoDetalleSalidaProduccionId()).thenReturn(10L);
        when(tipoMovimientoDetalleRepository.findById(10L)).thenReturn(Optional.of(tipoMovimientoDetalle(10L)));
        when(catalogResolver.getMotivoSalidaProduccionId()).thenReturn(11L);
        when(motivoMovimientoRepository.findById(11L)).thenReturn(Optional.of(motivoMovimiento(11L)));
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(6L);

        MovimientoInventario alistado = transferenciaPrebodega(insumo, 6, new BigDecimal("9"));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()), eq(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION), any()))
                .thenReturn(new PageImpl<>(List.of(alistado)));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                eq(orden.getId()), eq(ClasificacionMovimientoInventario.SALIDA_PRODUCCION), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(movimientoInventarioRepository.sumaCantidadPorOrdenProductoTipoDetalle(
                eq(orden.getId()), eq(insumo.getId().longValue()), eq(TipoMovimiento.SALIDA), eq(10L)))
                .thenReturn(new BigDecimal("4.5"));

        ReflectionTestUtils.invokeMethod(
                service,
                "registrarConsumoRealPorCierreTotal",
                orden,
                List.of(),
                null,
                usuario
        );

        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    private OrdenProduccion ordenProduccion(BigDecimal cantidadProgramada) {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setProducto(producto(100));
        orden.setCantidadProgramada(cantidadProgramada);
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        orden.setTipoCierre(TipoCierre.TOTAL);
        return orden;
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombreCompleto("Tester");
        return usuario;
    }

    private Producto producto(Integer id) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCategoriaProducto(new CategoriaProducto());
        return producto;
    }

    private FormulaProducto formula(Producto insumo, BigDecimal cantidad) {
        FormulaProducto formula = new FormulaProducto();
        DetalleFormula detalle = new DetalleFormula();
        detalle.setInsumo(insumo);
        detalle.setCantidadNecesaria(cantidad);
        formula.setDetalles(List.of(detalle));
        return formula;
    }

    private MovimientoInventario transferenciaPrebodega(Producto producto, int almacenDestino, BigDecimal cantidad) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        mov.setClasificacion(ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION);
        mov.setCantidad(cantidad);
        mov.setProducto(producto);
        LoteProducto lote = new LoteProducto();
        lote.setId(700L);
        lote.setCodigoLote("LOTE-X");
        lote.setProducto(producto);
        lote.setAlmacen(new Almacen(almacenDestino));
        mov.setLote(lote);
        mov.setFechaIngreso(LocalDateTime.now());
        return mov;
    }

    private TipoMovimientoDetalle tipoMovimientoDetalle(Long id) {
        TipoMovimientoDetalle detalle = new TipoMovimientoDetalle();
        detalle.setId(id);
        return detalle;
    }

    private MotivoMovimiento motivoMovimiento(Long id) {
        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(id);
        motivo.setMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        return motivo;
    }
}
