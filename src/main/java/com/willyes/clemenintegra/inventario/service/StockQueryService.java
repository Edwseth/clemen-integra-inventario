package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.StockDisponibleProjection;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockQueryService {
    private final ProductoRepository productoRepository;

    public Map<Long, BigDecimal> obtenerStockDisponible(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StockDisponibleProjection> rows = productoRepository.calcularStockDisponiblePorProducto(ids);
        return rows.stream()
                .collect(Collectors.toMap(StockDisponibleProjection::getProductoId,
                                          StockDisponibleProjection::getStockDisponible));
    }

    public BigDecimal obtenerStockDisponible(Long id) {
        return obtenerStockDisponible(List.of(id)).getOrDefault(id, BigDecimal.ZERO);
    }
}
