package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MotivoMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.inventario.repository.TipoMovimientoDetalleRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.ReservaLoteService;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.inventario.service.UmValidator;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaPlantillaRepository;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoDetalle;
import com.willyes.clemenintegra.produccion.service.model.DistribucionFefoResult;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenProduccionServicePsFefoTest {

    @Mock private FormulaProductoRepository formulaProductoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private SolicitudMovimientoService solicitudMovimientoService;
    @Mock private OrdenProduccionRepository ordenProduccionRepository;
    @Mock private MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock private TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock private EtapaProduccionRepository etapaProduccionRepository;
    @Mock private EtapaPlantillaRepository etapaPlantillaRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private UmValidator umValidator;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;
    @Mock private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private OrdenProduccionServiceImpl service;

    private OrdenProduccion orden;
    private Producto productoPs;
    private DetalleFormula insumoPs;

    @BeforeEach
    void init() {
        Producto productoPt = new Producto();
        productoPt.setId(100);
        CategoriaProducto categoriaPt = new CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        productoPt.setCategoriaProducto(categoriaPt);
        UnidadMedida umPt = new UnidadMedida();
        umPt.setSimbolo("UND");
        productoPt.setUnidadMedida(umPt);

        orden = new OrdenProduccion();
        orden.setProducto(productoPt);
        orden.setCantidadProgramada(new BigDecimal("10"));
        orden.setEstado(EstadoProduccion.CREADA);

        productoPs = new Producto();
        productoPs.setId(200);
        CategoriaProducto categoriaPs = new CategoriaProducto();
        categoriaPs.setTipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        productoPs.setCategoriaProducto(categoriaPs);
        UnidadMedida umPs = new UnidadMedida();
        umPs.setSimbolo("KG");
        productoPs.setUnidadMedida(umPs);
        productoPs.setModoControlInventario(ModoControlInventario.CONTROL_STOCK);

        insumoPs = new DetalleFormula();
        insumoPs.setInsumo(productoPs);
        insumoPs.setCantidadNecesaria(BigDecimal.ONE);

        FormulaProducto formula = new FormulaProducto();
        formula.setDetalles(List.of(insumoPs));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(100L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        when(productoRepository.findAllById(anyList())).thenReturn(List.of(productoPs));
        when(productoRepository.findById(100L)).thenReturn(Optional.of(productoPt));

        EtapaPlantilla plantilla = EtapaPlantilla.builder().id(1L).nombre("Mezcla").secuencia(1).build();
        when(etapaPlantillaRepository.findByProductoIdAndActivoTrueOrderBySecuenciaAsc(100))
                .thenReturn(List.of(plantilla));
        when(etapaProduccionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> {
            OrdenProduccion op = invocation.getArgument(0);
            op.setId(999L);
            return op;
        });
        when(ordenProduccionRepository.findById(999L)).thenReturn(Optional.of(orden));

        ReflectionTestUtils.setField(service, "estadosSolicitudPendientesConf", "PENDIENTE");
        ReflectionTestUtils.setField(service, "estadosSolicitudConcluyentesConf", "EJECUTADA");
        ReflectionTestUtils.setField(service, "clasificacionEntradaPtConf", "ENTRADA_PRODUCTO_TERMINADO");

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);

        MotivoMovimiento motivo = new MotivoMovimiento();
        motivo.setId(55L);
        when(motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_PRODUCCION))
                .thenReturn(Optional.of(motivo));
        TipoMovimientoDetalle tipoDetalle = new TipoMovimientoDetalle();
        tipoDetalle.setId(77L);
        when(tipoMovimientoDetalleRepository.findById(anyLong())).thenReturn(Optional.of(tipoDetalle));

        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(500L);
        when(catalogResolver.getAlmacenOrigenProductoSemiElaboradoId()).thenReturn(400L);
        when(catalogResolver.getTipoDetalleSalidaId()).thenReturn(77L);

        lenient().when(disponibilidadInsumoService.resolverAlmacenesPreferidos(productoPs))
                .thenReturn(List.of(400L));

        lenient().when(solicitudMovimientoRepository.findWithDetalles(eq(999L), any(), eq(null), eq(null), eq(false), anyList()))
                .thenReturn(List.of());

        lenient().when(solicitudMovimientoRepository.findById(anyLong()))
                .thenReturn(Optional.of(SolicitudMovimiento.builder()
                        .id(300L)
                        .estado(EstadoSolicitudMovimiento.PENDIENTE)
                        .detalles(new java.util.ArrayList<>())
                        .build()));
        lenient().doNothing().when(reservaLoteService).sincronizarReservasSolicitud(any(SolicitudMovimiento.class));

        lenient().when(solicitudMovimientoService.registrarSolicitud(any(SolicitudMovimientoRequestDTO.class)))
                .thenReturn(SolicitudMovimientoResponseDTO.builder().id(300L).build());

        when(umValidator.ajustar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(umValidator.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        when(catalogResolver.decimals(any())).thenReturn(2);
    }

    @Test
    @DisplayName("Crea OP con PS sin lote explícito eligiendo FEFO en un solo lote")
    void crearOp_psAutoSeleccion() {
        DistribucionFefoResult preview = fefoResult(true, 501L);
        DistribucionFefoResult reserva = fefoResult(false, 501L);

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(200L), any(BigDecimal.class), anyList(), eq(true), isNull(), eq(true)))
                .thenReturn(preview);
        when(disponibilidadInsumoService.calcularDisponibilidad(eq(200L), any(BigDecimal.class), anyList(), eq(false), isNull(), eq(true)))
                .thenReturn(reserva);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isTrue();
        assertThat(resultado.getOrden()).isNotNull();
        verify(disponibilidadInsumoService, times(1))
                .calcularDisponibilidad(eq(200L), any(BigDecimal.class), anyList(), eq(true), isNull(), eq(true));
        verify(disponibilidadInsumoService, times(1))
                .calcularDisponibilidad(eq(200L), any(BigDecimal.class), anyList(), eq(false), isNull(), eq(true));
    }

    @Test
    @DisplayName("Rechaza OP por stock insuficiente de PS sin exigir selección manual")
    void crearOp_psSinStockDisponible() {
        DistribucionFefoResult preview = DistribucionFefoResult.builder()
                .productoInsumoId(200L)
                .requerido(new BigDecimal("10.000000"))
                .stockLibreTotal(BigDecimal.ZERO)
                .stockFisicoTotal(BigDecimal.ZERO)
                .stockReservadoTotal(BigDecimal.ZERO)
                .faltante(new BigDecimal("10.000000"))
                .suficiente(false)
                .detalles(List.of())
                .build();

        when(disponibilidadInsumoService.calcularDisponibilidad(eq(200L), any(BigDecimal.class), anyList(), eq(true), isNull(), eq(true)))
                .thenReturn(preview);

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isFalse();
        assertThat(resultado.getInsumosFaltantes()).isNotEmpty();
        verify(ordenProduccionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Valida que el lote forzado sea PS")
    void crearOp_loteNoPs() {
        LoteProducto lote = new LoteProducto();
        lote.setId(900L);
        Producto otroProducto = new Producto();
        CategoriaProducto catMp = new CategoriaProducto();
        catMp.setTipo(TipoCategoria.MATERIA_PRIMA);
        otroProducto.setCategoriaProducto(catMp);
        lote.setProducto(otroProducto);
        orden.setLotePsId(900L);

        when(loteProductoRepository.findById(900L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.guardarConValidacionStock(orden))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("no corresponde a un producto semielaborado");
    }

    @Test
    @DisplayName("Valida que el lote PS esté LIBERADO")
    void crearOp_lotePsNoLiberado() {
        LoteProducto lote = new LoteProducto();
        lote.setId(901L);
        lote.setEstado(EstadoLote.DISPONIBLE);
        lote.setProducto(productoPs);
        orden.setLotePsId(901L);

        when(loteProductoRepository.findById(901L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.guardarConValidacionStock(orden))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("debe estar LIBERADO");
    }

    private DistribucionFefoResult fefoResult(boolean preview, Long loteId) {
        return DistribucionFefoResult.builder()
                .productoInsumoId(200L)
                .requerido(new BigDecimal("10.000000"))
                .stockLibreTotal(new BigDecimal("20.000000"))
                .stockFisicoTotal(new BigDecimal("20.000000"))
                .stockReservadoTotal(BigDecimal.ZERO)
                .faltante(BigDecimal.ZERO)
                .suficiente(true)
                .detalles(List.of(DistribucionFefoDetalle.builder()
                        .loteProductoId(loteId)
                        .almacenId(400L)
                        .cantidadCalculo(new BigDecimal("10.00000000"))
                        .cantidadReserva(new BigDecimal("10.000000"))
                        .disponible(new BigDecimal("10.000000"))
                        .estado(EstadoLote.LIBERADO.name())
                        .build()))
                .build();
    }
}
