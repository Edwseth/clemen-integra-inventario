package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenProduccionService {

    private final FormulaProductoRepository formulaProductoRepository;
    private final ProductoRepository productoRepository;

    @Autowired
    private OrdenProduccionRepository repository;

    @Transactional
    public OrdenProduccion guardarConValidacionStock(OrdenProduccion orden) {
        Long productoId = orden.getProducto().getId();

        // 1. Buscar fórmula asociada al producto
        FormulaProducto formula = formulaProductoRepository.findByProductoId(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una fórmula asociada al producto"));

        Map<Long, Producto> insumosMap = new HashMap<>();

        // 2. Verificar stock suficiente para cada insumo
        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId();
            Producto producto = productoRepository.findById(insumoId)
                    .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado: ID " + insumoId));

            BigDecimal stockActual = producto.getStockActual();
            BigDecimal cantidadNecesaria = insumo.getCantidadNecesaria();

            if (stockActual.compareTo(cantidadNecesaria) < 0) {
                throw new IllegalStateException("Stock insuficiente para el insumo: " + producto.getNombre());
            }

            insumosMap.put(insumoId, producto);
        }

        // 3. Descontar stock si todo está OK
        for (DetalleFormula insumo : formula.getDetalles()) {
            Long insumoId = insumo.getInsumo().getId();
            Producto producto = insumosMap.get(insumoId);
            BigDecimal nuevoStock = producto.getStockActual().subtract(insumo.getCantidadNecesaria());
            producto.setStockActual(nuevoStock);
            productoRepository.save(producto);
        }

        // 4. Guardar la orden
        return repository.save(orden);
    }

    public List<OrdenProduccion> listarTodas() {
        return repository.findAll();
    }

    public Optional<OrdenProduccion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
