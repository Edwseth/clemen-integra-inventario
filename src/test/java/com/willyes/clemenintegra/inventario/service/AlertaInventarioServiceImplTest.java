package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AlertaInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioSeveridad;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioTipo;
import com.willyes.clemenintegra.inventario.dto.LoteAlertaActivaProjection;
import com.willyes.clemenintegra.inventario.dto.LoteEstadoProlongadoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.StockAlertaProjection;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
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

    @Test
    @DisplayName("No genera alerta de stock máximo cuando el umbral no está configurado o es cero")
    void noGeneraStockMaximoCuandoUmbralNoConfigurado() {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of(
                new StockRow(1L, "Producto A", "SKU-A", 10L, "Almacén 1",
                        BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN)
        ));
        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of());

        List<AlertaInventarioResponseDTO> alertas = service.obtenerAlertasInventario(15);

        assertThat(alertas)
                .extracting(AlertaInventarioResponseDTO::getTipo)
                .doesNotContain(AlertaInventarioTipo.STOCK_MAXIMO);
    }

    @Test
    @DisplayName("Genera alerta de stock máximo cuando el umbral está configurado y el stock lo supera")
    void generaStockMaximoConUmbralConfigurado() {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of(
                new StockRow(1L, "Producto A", "SKU-A", 10L, "Almacén 1",
                        BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.TEN)
        ));
        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of());

        List<AlertaInventarioResponseDTO> alertas = service.obtenerAlertasInventario(15);

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.STOCK_MAXIMO)
                .singleElement()
                .satisfies(alerta -> {
                    assertThat(alerta.getStockActual()).isEqualByComparingTo("10");
                    assertThat(alerta.getUmbral()).isEqualByComparingTo("5");
                });
    }

    @Test
    @DisplayName("El stock actual del lote en alertas de vencimiento nunca es negativo")
    void stockActualDeLoteNoEsNegativo() {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of());
        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of(
                new LoteRow(201L, "L-NEG", LocalDateTime.parse("2025-01-10T00:00:00"), 5L, "Producto Neg", "SKU-N",
                        20L, "Almacén N", BigDecimal.valueOf(-5))
        ));

        List<AlertaInventarioResponseDTO> alertas = service.obtenerAlertasInventario(10);

        assertThat(alertas)
                .filteredOn(a -> a.getTipo() == AlertaInventarioTipo.LOTE_VENCIDO)
                .singleElement()
                .satisfies(alerta -> assertThat(alerta.getStockActual()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Filtra alertas activas por tipo y almacén")
    void filtraAlertasPorTipoYAlmacen() {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of(
                new StockRow(1L, "Producto A", "SKU-A", 10L, "Almacén 1",
                        BigDecimal.valueOf(5), BigDecimal.valueOf(20), BigDecimal.valueOf(3)),
                new StockRow(2L, "Producto B", "SKU-B", 11L, "Almacén 2",
                        BigDecimal.valueOf(1), BigDecimal.valueOf(8), BigDecimal.valueOf(10))
        ));
        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of(
                new LoteRow(101L, "L-1", LocalDateTime.parse("2025-01-10T00:00:00"), 1L, "Producto A", "SKU-A",
                        10L, "Almacén 1", BigDecimal.ONE),
                new LoteRow(102L, "L-2", LocalDateTime.parse("2025-01-08T00:00:00"), 2L, "Producto B", "SKU-B",
                        11L, "Almacén 2", BigDecimal.ONE)
        ));

        List<AlertaInventarioResponseDTO> alertas = service.obtenerAlertasInventario(30, AlertaInventarioTipo.LOTE_VENCIDO, 10L);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).getTipo()).isEqualTo(AlertaInventarioTipo.LOTE_VENCIDO);
        assertThat(alertas.get(0).getAlmacenId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Exporta Excel de inventario aplicando filtro tipo=LOTE_VENCIDO")
    void exportaExcelAplicandoFiltroTipo() throws Exception {
        when(loteProductoRepository.sumarStockParaAlertas()).thenReturn(List.of(
                new StockRow(1L, "Producto A", "SKU-A", 10L, "Almacén 1",
                        BigDecimal.valueOf(5), BigDecimal.valueOf(20), BigDecimal.valueOf(3))
        ));
        when(loteProductoRepository.listarLotesConVencimiento()).thenReturn(List.of(
                new LoteRow(101L, "L-1", LocalDateTime.parse("2025-01-10T00:00:00"), 1L, "Producto A", "SKU-A",
                        10L, "Almacén 1", BigDecimal.ONE),
                new LoteRow(102L, "L-2", LocalDateTime.parse("2025-01-20T00:00:00"), 1L, "Producto A", "SKU-A",
                        10L, "Almacén 1", BigDecimal.ONE)
        ));

        byte[] excel = service.generarReporteAlertasInventarioExcel(30, AlertaInventarioTipo.LOTE_VENCIDO, null);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            var sheet = workbook.getSheet("Alertas Inventario");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("LOTE_VENCIDO");
        }
    }

    @Test
    @DisplayName("Incluye estadoCalidadResumen en lotes retenidos o en cuarentena prolongados")
    void lotesRetenidosProlongadosIncluyenEstadoCalidadResumen() {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setNombre("Producto A");

        LoteProducto lote = new LoteProducto();
        lote.setId(501L);
        lote.setCodigoLote("L-RET");
        lote.setProducto(producto);
        lote.setEstado(EstadoLote.RETENIDO);
        lote.setFechaFabricacion(LocalDateTime.parse("2025-01-01T00:00:00"));

        when(loteProductoRepository.findAll()).thenReturn(List.of(lote));

        List<LoteEstadoProlongadoResponseDTO> resultado = service.obtenerLotesRetenidosOCuarentenaProlongados();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstadoCalidadResumen()).isEqualTo(EstadoLote.RETENIDO.name());
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
