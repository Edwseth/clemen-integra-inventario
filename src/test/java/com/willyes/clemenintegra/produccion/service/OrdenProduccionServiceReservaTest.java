package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.inventario.dto.LoteFefoDisponibleProjection;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.TipoMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
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
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    @Mock private VidaUtilProductoRepository vidaUtilProductoRepository;
    @Mock private ReservaLoteService reservaLoteService;

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
        producto.setTipoAnalisis(com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad.NINGUNO);
        com.willyes.clemenintegra.inventario.model.CategoriaProducto categoriaPt = new com.willyes.clemenintegra.inventario.model.CategoriaProducto();
        categoriaPt.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setCategoriaProducto(categoriaPt);
        producto.setNombre("Producto Final");
        orden.setProducto(producto);
        orden.setCantidadProgramada(new BigDecimal("5.5"));

        productoInsumo = new Producto();
        productoInsumo.setId(50);
        productoInsumo.setNombre("INSUMO-X");
        productoInsumo.setCategoriaProducto(new com.willyes.clemenintegra.inventario.model.CategoriaProducto());
        productoInsumo.getCategoriaProducto().setTipo(TipoCategoria.MATERIA_PRIMA);

        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenAnswer(invocation -> Optional.of(crearFormula()));
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), any(), eq(null), eq(null)))
                .thenReturn(List.of());
        when(catalogResolver.getAlmacenPreBodegaProduccionId()).thenReturn(99L);
        when(catalogResolver.getAlmacenOrigenMateriaPrimaId()).thenReturn(5L);
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
        when(loteProductoRepository.findFefoDisponibles(50L, Integer.MAX_VALUE))
                .thenReturn(List.of(
                        loteFefo(200L, new BigDecimal("3.123456"), "DISPONIBLE"),
                        loteFefo(201L, new BigDecimal("10.000000"), "LIBERADO")
                ));

        service.reservarInsumosParaOP(1L);

        ArgumentCaptor<SolicitudMovimientoRequestDTO> dtoCaptor = ArgumentCaptor.forClass(SolicitudMovimientoRequestDTO.class);
        verify(solicitudMovimientoService).registrarSolicitud(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getCantidad()).isEqualByComparingTo(new BigDecimal("6.79"));

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
    @DisplayName("reservarInsumosParaOP falla con 422 cuando no hay lotes elegibles")
    void reservarInsumosParaOp_sinLotesLanzaError() {
        when(loteProductoRepository.findFefoDisponibles(50L, Integer.MAX_VALUE)).thenReturn(List.of());
        assertThatThrownBy(() -> service.reservarInsumosParaOP(1L))
                .isInstanceOf(ResponseStatusException.class)
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
                .build();

        when(umValidator.ajustar(any(BigDecimal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(umValidator.getRoundingMode()).thenReturn(RoundingMode.HALF_UP);
        when(catalogResolver.decimals(any(UnidadMedida.class))).thenReturn(6);
        when(catalogResolver.getMotivoIdDevolucionDesdeProduccion()).thenReturn(70L);
        MotivoMovimiento motivoDev = new MotivoMovimiento();
        motivoDev.setId(70L);
        when(motivoMovimientoRepository.findById(70L)).thenReturn(Optional.of(motivoDev));
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), eq(null), eq(null), eq(null)))
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
        when(vidaUtilProductoRepository.findById(10)).thenReturn(Optional.of(VidaUtilProducto.builder()
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
        when(solicitudMovimientoRepository.findWithDetalles(eq(1L), any(), eq(null), eq(null)))
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

    private SolicitudMovimiento crearSolicitudBase() {
        SolicitudMovimiento solicitud = new SolicitudMovimiento();
        solicitud.setId(100L);
        solicitud.setDetalles(new ArrayList<>());
        Almacen destino = new Almacen();
        destino.setId(9);
        solicitud.setAlmacenDestino(destino);
        return solicitud;
    }

    private LoteFefoDisponibleProjection loteFefo(Long id, BigDecimal stock, String estado) {
        return new LoteFefoDisponibleProjection() {
            @Override
            public Long getLoteProductoId() {
                return id;
            }

            @Override
            public String getCodigoLote() {
                return "L" + id;
            }

            @Override
            public BigDecimal getStockLote() {
                return stock;
            }

            @Override
            public LocalDateTime getFechaVencimiento() {
                return LocalDateTime.now().plusDays(30);
            }

            @Override
            public Long getAlmacenId() {
                return 5L;
            }

            @Override
            public String getNombreAlmacen() {
                return "MP";
            }

            @Override
            public String getEstado() {
                return estado;
            }
        };
    }
}
