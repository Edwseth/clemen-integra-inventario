package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AlertaInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioSeveridad;
import com.willyes.clemenintegra.inventario.dto.AlertaInventarioTipo;
import com.willyes.clemenintegra.inventario.dto.LoteAlertaActivaProjection;
import com.willyes.clemenintegra.inventario.dto.LoteAlertaResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LoteEstadoProlongadoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoAlertaResponseDTO;
import com.willyes.clemenintegra.inventario.dto.StockAlertaProjection;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertaInventarioServiceImpl implements AlertaInventarioService {

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteProductoRepository;
    private final StockQueryService stockQueryService;
    private Clock clock = Clock.systemDefaultZone();

    private static final int DIAS_VENCIMIENTO_POR_DEFECTO = 30;

    public List<ProductoAlertaResponseDTO> obtenerProductosConStockBajo() {
        List<Producto> productos = productoRepository.findAll();
        Map<Long, BigDecimal> stockMap = stockQueryService.obtenerStockDisponible(
                productos.stream().map(p -> p.getId().longValue()).toList());
        return productos.stream()
                .filter(p -> stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)
                        .compareTo(p.getStockMinimo()) < 0)
                .map(p -> mapToResponse(p, stockMap.getOrDefault(p.getId().longValue(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    private ProductoAlertaResponseDTO mapToResponse(Producto producto, BigDecimal stockDisponible) {
        return ProductoAlertaResponseDTO.builder()
                .productoId(producto.getId().longValue())
                .nombreProducto(producto.getNombre())
                .stockDisponible(stockDisponible)
                .stockMinimo(producto.getStockMinimo())
                .build();
    }

    public List<LoteAlertaResponseDTO> obtenerLotesVencidos() {
        return loteProductoRepository.findAll().stream()
                .filter(lote -> lote.getFechaVencimiento() != null
                        && lote.getFechaVencimiento().isBefore(LocalDateTime.now(clock)))
                .filter(lote -> lote.getProducto() != null)
                .map(lote -> LoteAlertaResponseDTO.builder()
                        .loteId(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .fechaVencimiento(lote.getFechaVencimiento())
                        .nombreProducto(lote.getProducto().getNombre())
                        .nombreAlmacen(lote.getAlmacen().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public List<LoteEstadoProlongadoResponseDTO> obtenerLotesRetenidosOCuarentenaProlongados() {
        return loteProductoRepository.findAll().stream()
                .filter(lote ->
                        lote.getEstado() != null &&
                                (lote.getEstado().name().equals("EN_CUARENTENA") || lote.getEstado().name().equals("RETENIDO")) &&
                                lote.getFechaFabricacion() != null &&
                                lote.getFechaFabricacion().isBefore(LocalDate.now(clock).minusDays(10).atStartOfDay())  // Cambia a 10 días para prolongados, campo para modificar la alerta
                )
                .filter(lote -> lote.getProducto() != null)
                .map(lote -> LoteEstadoProlongadoResponseDTO.builder()
                        .loteId(lote.getId())
                        .codigoLote(lote.getCodigoLote())
                        .estado(lote.getEstado().name())
                        .estadoCalidadResumen(lote.getEstadoCalidadResumen())
                        .fechaFabricacion(lote.getFechaFabricacion())
                        .diasEnEstado((int) ChronoUnit.DAYS.between(lote.getFechaFabricacion(), LocalDate.now(clock).atStartOfDay()))
                        .nombreProducto(lote.getProducto().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public List<AlertaInventarioResponseDTO> obtenerAlertasInventario(Integer diasVencimiento) {
        return obtenerAlertasInventario(diasVencimiento, null, null);
    }

    @Override
    public List<AlertaInventarioResponseDTO> obtenerAlertasInventario(Integer diasVencimiento,
                                                                      AlertaInventarioTipo tipo,
                                                                      Long almacenId) {
        int diasUmbral = (diasVencimiento == null || diasVencimiento < 0) ? DIAS_VENCIMIENTO_POR_DEFECTO : diasVencimiento;
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime corteProximoVencer = ahora.plusDays(diasUmbral);

        List<AlertaInventarioResponseDTO> alertas = new java.util.ArrayList<>();

        for (StockAlertaProjection stock : loteProductoRepository.sumarStockParaAlertas()) {
            BigDecimal stockActual = defaultBigDecimal(stock.getStockActual());
            BigDecimal stockMinimo = defaultBigDecimal(stock.getStockMinimo());
            BigDecimal stockMaximo = stock.getStockMaximoPlaneacion();

            if (stockActual.compareTo(stockMinimo) < 0) {
                alertas.add(AlertaInventarioResponseDTO.builder()
                        .tipo(AlertaInventarioTipo.STOCK_MINIMO)
                        .severidad(AlertaInventarioSeveridad.ADVERTENCIA)
                        .productoId(stock.getProductoId())
                        .nombreProducto(stock.getNombreProducto())
                        .codigoSku(stock.getCodigoSku())
                        .almacenId(stock.getAlmacenId())
                        .nombreAlmacen(stock.getNombreAlmacen())
                        .stockActual(stockActual)
                        .umbral(stockMinimo)
                        .mensaje(String.format("Stock actual %s por debajo del mínimo %s", stockActual.toPlainString(), stockMinimo.toPlainString()))
                        .build());
            }

            if (stockMaximo != null && stockMaximo.compareTo(BigDecimal.ZERO) > 0 && stockActual.compareTo(stockMaximo) > 0) {
                alertas.add(AlertaInventarioResponseDTO.builder()
                        .tipo(AlertaInventarioTipo.STOCK_MAXIMO)
                        .severidad(AlertaInventarioSeveridad.ADVERTENCIA)
                        .productoId(stock.getProductoId())
                        .nombreProducto(stock.getNombreProducto())
                        .codigoSku(stock.getCodigoSku())
                        .almacenId(stock.getAlmacenId())
                        .nombreAlmacen(stock.getNombreAlmacen())
                        .stockActual(stockActual)
                        .umbral(stockMaximo)
                        .mensaje(String.format("Stock actual %s supera el máximo %s", stockActual.toPlainString(), stockMaximo.toPlainString()))
                        .build());
            }
        }

        for (LoteAlertaActivaProjection lote : loteProductoRepository.listarLotesConVencimiento()) {
            LocalDateTime fechaVencimiento = lote.getFechaVencimiento();
            if (fechaVencimiento == null) {
                continue;
            }

            BigDecimal stockActual = defaultBigDecimal(lote.getStockActual()).max(BigDecimal.ZERO);
            if (fechaVencimiento.isBefore(ahora)) {
                alertas.add(AlertaInventarioResponseDTO.builder()
                        .tipo(AlertaInventarioTipo.LOTE_VENCIDO)
                        .severidad(AlertaInventarioSeveridad.CRITICA)
                        .productoId(lote.getProductoId())
                        .nombreProducto(lote.getNombreProducto())
                        .codigoSku(lote.getCodigoSku())
                        .almacenId(lote.getAlmacenId())
                        .nombreAlmacen(lote.getNombreAlmacen())
                        .loteProductoId(lote.getLoteProductoId())
                        .codigoLote(lote.getCodigoLote())
                        .fechaVencimiento(fechaVencimiento)
                        .stockActual(stockActual)
                        .umbral(BigDecimal.ZERO)
                        .mensaje("Lote vencido")
                        .build());
            } else if (!fechaVencimiento.isAfter(corteProximoVencer)) {
                alertas.add(AlertaInventarioResponseDTO.builder()
                        .tipo(AlertaInventarioTipo.LOTE_POR_VENCER)
                        .severidad(AlertaInventarioSeveridad.ADVERTENCIA)
                        .productoId(lote.getProductoId())
                        .nombreProducto(lote.getNombreProducto())
                        .codigoSku(lote.getCodigoSku())
                        .almacenId(lote.getAlmacenId())
                        .nombreAlmacen(lote.getNombreAlmacen())
                        .loteProductoId(lote.getLoteProductoId())
                        .codigoLote(lote.getCodigoLote())
                        .fechaVencimiento(fechaVencimiento)
                        .stockActual(stockActual)
                        .umbral(BigDecimal.valueOf(diasUmbral))
                        .mensaje(String.format("Lote próximo a vencer en %d días o menos", diasUmbral))
                        .build());
            }
        }

        return alertas.stream()
                .filter(alerta -> tipo == null || alerta.getTipo() == tipo)
                .filter(alerta -> almacenId == null || Objects.equals(alerta.getAlmacenId(), almacenId))
                .toList();
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    @Override
    public byte[] generarReporteAlertasInventarioExcel(Integer diasVencimiento) {
        return generarReporteAlertasInventarioExcel(diasVencimiento, null, null);
    }

    @Override
    public byte[] generarReporteAlertasInventarioExcel(Integer diasVencimiento,
                                                       AlertaInventarioTipo tipo,
                                                       Long almacenId) {
        List<AlertaInventarioResponseDTO> alertas = obtenerAlertasInventario(diasVencimiento, tipo, almacenId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alertas Inventario");
            String[] headers = {
                    "Tipo Alerta", "Severidad", "SKU", "Producto", "Almacén",
                    "Código Lote", "Fecha Vencimiento", "Stock Actual", "Umbral", "Mensaje"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (AlertaInventarioResponseDTO alerta : alertas) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(alerta.getTipo() != null ? alerta.getTipo().name() : "");
                row.createCell(1).setCellValue(alerta.getSeveridad() != null ? alerta.getSeveridad().name() : "");
                row.createCell(2).setCellValue(alerta.getCodigoSku() != null ? alerta.getCodigoSku() : "");
                row.createCell(3).setCellValue(alerta.getNombreProducto() != null ? alerta.getNombreProducto() : "");
                row.createCell(4).setCellValue(alerta.getNombreAlmacen() != null ? alerta.getNombreAlmacen() : "");
                row.createCell(5).setCellValue(alerta.getCodigoLote() != null ? alerta.getCodigoLote() : "");
                row.createCell(6).setCellValue(alerta.getFechaVencimiento() != null ? alerta.getFechaVencimiento().format(dtf) : "");
                row.createCell(7).setCellValue(alerta.getStockActual() != null ? alerta.getStockActual().toPlainString() : "");
                row.createCell(8).setCellValue(alerta.getUmbral() != null ? alerta.getUmbral().toPlainString() : "");
                row.createCell(9).setCellValue(alerta.getMensaje() != null ? alerta.getMensaje() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel de alertas de inventario", e);
        }
    }

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }
}
