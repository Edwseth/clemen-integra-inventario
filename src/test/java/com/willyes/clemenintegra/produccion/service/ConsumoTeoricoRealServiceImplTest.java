package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumoTeoricoRealServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private FormulaProductoRepository formulaProductoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;

    @InjectMocks
    private ConsumoTeoricoRealServiceImpl service;

    @Test
    @DisplayName("calcula diferencias en cero cuando real=teorico")
    void calcularConsumoRealIgualTeorico() {
        OrdenProduccion orden = ordenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        Producto insumo = insumoPrincipal();
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(anyLong(), any()))
                .thenReturn(Optional.of(formulaSimple(BigDecimal.ONE, insumo)));
        MovimientoInventario movimiento = movimiento(insumo, new BigDecimal("10"));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento)));
        when(loteProductoRepository.findByOrdenProduccionId(1L)).thenReturn(List.of());

        ConsumoTeoricoRealResponseDTO response = service.obtenerConsumo(1L);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getDiferencia()).isZero();
        assertThat(response.getItems().get(0).getPorcentajeDesviacion()).isZero();
    }

    @Test
    @DisplayName("maneja porcentaje nulo cuando cantidad teórica es cero")
    void manejarTeoricoCero() {
        OrdenProduccion orden = ordenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        Producto insumo = insumoPrincipal();
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(anyLong(), any()))
                .thenReturn(Optional.of(formulaSimple(BigDecimal.ZERO, insumo)));
        MovimientoInventario movimiento = movimiento(insumo, new BigDecimal("5"));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(anyLong(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento)));
        LoteProducto lote = new LoteProducto();
        lote.setId(5L);
        when(loteProductoRepository.findByOrdenProduccionId(1L)).thenReturn(List.of(lote));

        ConsumoTeoricoRealResponseDTO response = service.obtenerConsumo(1L);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPorcentajeDesviacion()).isNull();
        assertThat(response.getLotesTerminados()).hasSize(1);
    }

    private OrdenProduccion ordenProduccion() {
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setCantidadProgramada(new BigDecimal("10"));
        Producto producto = new Producto();
        producto.setId(2);
        producto.setCodigoSku("PT-01");
        producto.setNombre("Producto terminado");
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("UND");
        producto.setUnidadMedida(unidad);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.PRODUCTO_TERMINADO);
        producto.setCategoriaProducto(categoria);
        orden.setProducto(producto);
        return orden;
    }

    private FormulaProducto formulaSimple(BigDecimal cantidadNecesaria, Producto insumo) {
        FormulaProducto formula = new FormulaProducto();
        formula.setEstado(EstadoFormula.APROBADA);
        DetalleFormula detalle = new DetalleFormula();
        detalle.setCantidadNecesaria(cantidadNecesaria);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("KG");
        insumo.setUnidadMedida(unidad);
        detalle.setInsumo(insumo);
        detalle.setUnidadMedida(unidad);
        formula.setDetalles(List.of(detalle));
        return formula;
    }

    private Producto insumoPrincipal() {
        Producto insumo = new Producto();
        insumo.setId(10);
        insumo.setCodigoSku("INS-1");
        insumo.setNombre("Insumo principal");
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(TipoCategoria.MATERIA_PRIMA);
        insumo.setCategoriaProducto(categoria);
        return insumo;
    }

    private MovimientoInventario movimiento(Producto producto, BigDecimal cantidad) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setCantidad(cantidad);
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        return movimiento;
    }
}

