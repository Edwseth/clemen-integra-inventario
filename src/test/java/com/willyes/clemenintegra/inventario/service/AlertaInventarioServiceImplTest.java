package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AlertaInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioSeveridad;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioTipo;
import com.willyes.clemenintegra.inventario.dto.LoteAlertaActivaProjection;
import com.willyes.clemenintegra.inventario.dto.StockAlertaProjection;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaInventarioServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private StockQueryService stockQueryService;

    @InjectMocks
    private AlertaInventarioServiceImpl service;

    @BeforeEach
    void setUpClock() {
        service.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("Genera alertas por stock mínimo, stock máximo, lote por vencer y lote vencido sin filtrar por estado")
    void generaAlertasActivasCompleta() {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of(
                new StockRow(1L, "Producto A", "SKU-A", 10L, "Almacén 1",
                        BigDecimal.valueOf(5), BigDecimal.valueOf(20), BigDecimal.valueOf(3)),
                new StockRow(2L, "Producto B", "SKU-B", 11L, "Almacén 2",
                        BigDecimal.valueOf(5), BigDecimal.valueOf(8), BigDecimal.valueOf(12))
        ));

        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of(
                new LoteRow(101L, "L-1", LocalDateTime.parse("2025-01-10T00:00:00"), 1L, "Producto A", "SKU-A",
                        10L, "Almacén 1", BigDecimal.valueOf(1)),
                new LoteRow(102L, "L-2", LocalDateTime.parse("2025-02-10T00:00:00"), 2L, "Producto B", "SKU-B",
                        11L, "Almacén 2", BigDecimal.valueOf(2)),
                new LoteRow(103L, "L-3", LocalDateTime.parse("2025-03-20T00:00:00"), 3L, "Producto C", "SKU-C",
                        12L, "Almacén 3", BigDecimal.ONE)
        ));

        List<AlertaInventarioResponseDTO> alertas = service.obtenerAlertasInventario(30);

        assertThat(alertas)
                .hasSize(4)
                .extracting(AlertaInventarioResponseDTO::getTipo)
                .containsExactlyInAnyOrder(
                        AlertaInventarioTipo.STOCK_MINIMO,
                        AlertaInventarioTipo.STOCK_MAXIMO,
                        AlertaInventarioTipo.LOTE_VENCIDO,
                        AlertaInventarioTipo.LOTE_POR_VENCER
                );

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.STOCK_MINIMO)
                .first()
                .satisfies(alerta -> {
                    assertThat(alerta.getStockActual()).isEqualByComparingTo("3");
                    assertThat(alerta.getUmbral()).isEqualByComparingTo("5");
                    assertThat(alerta.getSeveridad()).isEqualTo(AlertaInventarioSeveridad.ADVERTENCIA);
                });

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.STOCK_MAXIMO)
                .first()
                .satisfies(alerta -> {
                    assertThat(alerta.getStockActual()).isEqualByComparingTo("12");
                    assertThat(alerta.getUmbral()).isEqualByComparingTo("8");
                });

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.LOTE_VENCIDO)
                .first()
                .satisfies(alerta -> {
                    assertThat(alerta.getLoteProductoId()).isEqualTo(101L);
                    assertThat(alerta.getSeveridad()).isEqualTo(AlertaInventarioSeveridad.CRITICA);
                });

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.LOTE_POR_VENCER)
                .first()
                .satisfies(alerta -> {
                    assertThat(alerta.getLoteProductoId()).isEqualTo(102L);
                    assertThat(alerta.getUmbral()).isEqualByComparingTo("30");
                });
    }

    private record StockRow(Long productoId, String nombreProducto, String codigoSku, Long almacenId, String nombreAlmacen,
                            BigDecimal stockMinimo, BigDecimal stockMaximoPlaneacion, BigDecimal stockActual)
            implements StockAlertaProjection {
        @Override
        public Long getProductoId() {return productoId;}

        @Override
        public String getNombreProducto() {return nombreProducto;}

        @Override
        public String getCodigoSku() {return codigoSku;}

        @Override
        public Long getAlmacenId() {return almacenId;}

        @Override
        public String getNombreAlmacen() {return nombreAlmacen;}

        @Override
        public BigDecimal getStockMinimo() {return stockMinimo;}

        @Override
        public BigDecimal getStockMaximoPlaneacion() {return stockMaximoPlaneacion;}

        @Override
        public BigDecimal getStockActual() {return stockActual;}
    }

    private record LoteRow(Long loteProductoId, String codigoLote, LocalDateTime fechaVencimiento,
                           Long productoId, String nombreProducto, String codigoSku,
                           Long almacenId, String nombreAlmacen, BigDecimal stockActual)
            implements LoteAlertaActivaProjection {
        @Override
        public Long getLoteProductoId() {return loteProductoId;}

        @Override
        public String getCodigoLote() {return codigoLote;}

        @Override
        public LocalDateTime getFechaVencimiento() {return fechaVencimiento;}

        @Override
        public Long getProductoId() {return productoId;}

        @Override
        public String getNombreProducto() {return nombreProducto;}

        @Override
        public String getCodigoSku() {return codigoSku;}

        @Override
        public Long getAlmacenId() {return almacenId;}

        @Override
        public String getNombreAlmacen() {return nombreAlmacen;}

        @Override
        public BigDecimal getStockActual() {return stockActual;}
    }
}
