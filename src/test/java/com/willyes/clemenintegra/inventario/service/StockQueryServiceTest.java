package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
