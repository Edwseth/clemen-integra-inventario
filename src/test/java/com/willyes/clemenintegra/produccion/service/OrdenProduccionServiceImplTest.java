package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.*;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.*;
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
