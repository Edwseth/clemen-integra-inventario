package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.StockDisponibleProjection;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockQueryServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    private StockQueryService service;

    @BeforeEach
    void setUp() {
        service = new StockQueryService(productoRepository);
    }

    @Test
    void obtenerStockDisponibleReturnsEmptyMapWhenRepositoryReturnsNull() {
        List<Long> ids = List.of(1L, 2L);
        when(productoRepository.calcularStockDisponiblePorProducto(ids)).thenReturn(null);

        Map<Long, BigDecimal> result = service.obtenerStockDisponible(ids);

        assertTrue(result.isEmpty());
    }

    @Test
    void obtenerStockDisponibleIgnoresAgotados() {
        List<Long> ids = List.of(1L, 2L);
        StockDisponibleProjection row = new StockDisponibleProjection() {
            @Override
            public Long getProductoId() { return 1L; }
            @Override
            public BigDecimal getStockDisponible() { return BigDecimal.TEN; }
        };
        when(productoRepository.calcularStockDisponiblePorProducto(anyList()))
                .thenReturn(List.of(row));

        Map<Long, BigDecimal> result = service.obtenerStockDisponible(ids);

        assertEquals(BigDecimal.TEN, result.get(1L));
        assertFalse(result.containsKey(2L));
        assertEquals(BigDecimal.ZERO, result.getOrDefault(2L, BigDecimal.ZERO));
    }

    @Test
    void obtenerStockDisponibleEnAlmacenesIgnoresAgotados() {
        List<Long> ids = List.of(1L, 2L);
        List<Long> almacenes = List.of(5L);
        StockDisponibleProjection row = new StockDisponibleProjection() {
            @Override
            public Long getProductoId() { return 1L; }
            @Override
            public BigDecimal getStockDisponible() { return BigDecimal.ONE; }
        };
        when(productoRepository.calcularStockDisponiblePorProductoEnAlmacenes(anyList(), anyList()))
                .thenReturn(List.of(row));

        Map<Long, BigDecimal> result = service.obtenerStockDisponible(ids, almacenes);

        assertEquals(BigDecimal.ONE, result.get(1L));
        assertFalse(result.containsKey(2L));
        assertEquals(BigDecimal.ZERO, result.getOrDefault(2L, BigDecimal.ZERO));
    }
}
