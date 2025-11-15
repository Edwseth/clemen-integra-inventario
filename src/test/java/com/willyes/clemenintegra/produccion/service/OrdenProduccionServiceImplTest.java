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
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.*;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceImplTest {

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
    @Mock private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private UsuarioService usuarioService;
    @Mock private SolicitudMovimientoRepository solicitudMovimientoRepository;
    @Mock private InventoryCatalogResolver catalogResolver;
    @Mock private UmValidator umValidator;
    @Mock private VidaUtilProductoRepository vidaUtilProductoRepository;
    @Mock private ReservaLoteService reservaLoteService;
    @Mock private DisponibilidadInsumoService disponibilidadInsumoService;

    @InjectMocks
    private OrdenProduccionServiceImpl service;

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
        when(stockQueryService.obtenerStockDisponible(eq(List.of(2L)), eq(List.of(5L))))
                .thenReturn(Map.of(2L, new BigDecimal("8")));

        ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(orden);

        assertThat(resultado.isEsValida()).isFalse();
        assertThat(resultado.getUnidadesMaximasProducibles()).isEqualTo(8);
        assertThat(resultado.getInsumosFaltantes()).hasSize(1);
        assertThat(resultado.getInsumosFaltantes().get(0).getProductoId()).isEqualTo(2L);
        assertThat(resultado.getInsumosFaltantes().get(0).getRequerido()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(resultado.getInsumosFaltantes().get(0).getDisponible()).isEqualByComparingTo(new BigDecimal("8"));
    }
}
