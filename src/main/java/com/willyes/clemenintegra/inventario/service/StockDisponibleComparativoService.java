package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.StockDisponibleComparativoResponseDTO;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockDisponibleComparativoService {

    private final InventarioGeneralCorteReportService inventarioGeneralCorteReportService;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<StockDisponibleComparativoResponseDTO> obtenerComparativo(LocalDate fechaIndicada,
                                                                          Long categoriaId,
                                                                          String orden,
                                                                          String direccion) {
        LocalDate fechaActual = LocalDate.now();
        Set<Long> productoIds = resolverProductoIds(categoriaId);
        if (categoriaId != null && productoIds.isEmpty()) {
            return List.of();
        }

        List<InventarioGeneralCorteReportService.InventarioGeneralRow> rowsFecha =
                inventarioGeneralCorteReportService.calcularFilasInventarioGeneralCorte(fechaIndicada.atTime(LocalTime.MAX), productoIds);
        List<InventarioGeneralCorteReportService.InventarioGeneralRow> rowsActual =
                inventarioGeneralCorteReportService.calcularFilasInventarioGeneralCorte(fechaActual.atTime(LocalTime.MAX), productoIds);

        Map<ComparativoKey, BaseComparativo> merged = new LinkedHashMap<>();
        acumular(rowsFecha, merged, true);
        acumular(rowsActual, merged, false);

        Comparator<StockDisponibleComparativoResponseDTO> comparador = construirComparador(orden, direccion);

        return merged.values().stream()
                .map(BaseComparativo::toDto)
                .sorted(comparador)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StockDisponibleComparativoResponseDTO> obtenerComparativoPaginado(LocalDate fechaIndicada,
                                                                                   Long categoriaId,
                                                                                   String sortField,
                                                                                   String sortDir,
                                                                                   Integer page,
                                                                                   Integer size) {
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);

        List<StockDisponibleComparativoResponseDTO> resultados =
                obtenerComparativo(fechaIndicada, categoriaId, sortField, sortDir);

        int total = resultados.size();
        int desde = Math.min(pageNumber * pageSize, total);
        int hasta = Math.min(desde + pageSize, total);

        return new PageImpl<>(resultados.subList(desde, hasta), PageRequest.of(pageNumber, pageSize), total);
    }

    @Transactional(readOnly = true)
    public Workbook generarExcelComparativo(LocalDate fechaIndicada,
                                            Long categoriaId,
                                            String orden,
                                            String direccion) {
        List<StockDisponibleComparativoResponseDTO> filas = obtenerComparativo(fechaIndicada, categoriaId, orden, direccion);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock disponible comparativo");

        CreationHelper creationHelper = workbook.getCreationHelper();
        DataFormat dataFormat = creationHelper.createDataFormat();
        CellStyle numericStyle = workbook.createCellStyle();
        numericStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

        String[] columnas = {
                "Sku", "Nombre", "UDM", "Cantidad_Fecha_Indicada", "Cantidad_Actual", "Diferencia", "Lote", "Vence", "Ubicación"
        };

        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            header.createCell(i).setCellValue(columnas[i]);
        }

        int rowNum = 1;
        for (StockDisponibleComparativoResponseDTO fila : filas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(texto(fila.sku()));
            row.createCell(1).setCellValue(texto(fila.nombre()));
            row.createCell(2).setCellValue(texto(fila.udm()));

            var cFecha = row.createCell(3);
            cFecha.setCellValue(fila.cantidadFechaIndicada().doubleValue());
            cFecha.setCellStyle(numericStyle);

            var cActual = row.createCell(4);
            cActual.setCellValue(fila.cantidadActual().doubleValue());
            cActual.setCellStyle(numericStyle);

            var cDiferencia = row.createCell(5);
            cDiferencia.setCellValue(fila.diferencia().doubleValue());
            cDiferencia.setCellStyle(numericStyle);

            row.createCell(6).setCellValue(texto(fila.lote()));
            row.createCell(7).setCellValue(fila.vence() == null ? "" : fila.vence().toString());
            row.createCell(8).setCellValue(texto(fila.ubicacion()));
        }

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    private void acumular(List<InventarioGeneralCorteReportService.InventarioGeneralRow> rows,
                          Map<ComparativoKey, BaseComparativo> merged,
                          boolean fechaIndicada) {
        for (InventarioGeneralCorteReportService.InventarioGeneralRow row : rows) {
            ComparativoKey key = new ComparativoKey(normalizar(row.sku()), normalizar(row.lote()), normalizar(row.ubicacion()));
            BaseComparativo base = merged.computeIfAbsent(key, ignored -> new BaseComparativo(
                    row.sku(),
                    row.nombre(),
                    row.udm(),
                    row.lote(),
                    parseFecha(row.vence()),
                    row.ubicacion()
            ));
            if (fechaIndicada) {
                base.cantidadFechaIndicada = row.cant();
            } else {
                base.cantidadActual = row.cant();
            }
        }
    }

    private Set<Long> resolverProductoIds(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return Set.copyOf(productoRepository.findIdsByCategoriaProductoId(categoriaId));
    }

    private Comparator<StockDisponibleComparativoResponseDTO> construirComparador(String orden, String direccion) {
        Comparator<StockDisponibleComparativoResponseDTO> base = switch (normalizar(orden)) {
            case "sku" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::sku, Comparator.nullsLast(String::compareToIgnoreCase));
            case "udm" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::udm, Comparator.nullsLast(String::compareToIgnoreCase));
            case "cant", "cantidadfechaindicada" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::cantidadFechaIndicada,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "cantidadactual" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::cantidadActual,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "diferencia" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::diferencia,
                    Comparator.nullsLast(BigDecimal::compareTo));
            case "lote" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::lote, Comparator.nullsLast(String::compareToIgnoreCase));
            case "vence" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::vence, Comparator.nullsLast(LocalDate::compareTo));
            case "ubicacion" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::ubicacion, Comparator.nullsLast(String::compareToIgnoreCase));
            case "nombre", "" -> Comparator.comparing(StockDisponibleComparativoResponseDTO::nombre, Comparator.nullsLast(String::compareToIgnoreCase));
            default -> Comparator.comparing(StockDisponibleComparativoResponseDTO::nombre, Comparator.nullsLast(String::compareToIgnoreCase));
        };

        if ("desc".equals(normalizar(direccion))) {
            base = base.reversed();
        }

        Comparator<StockDisponibleComparativoResponseDTO> fallback = Comparator
                .comparing(StockDisponibleComparativoResponseDTO::nombre, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(StockDisponibleComparativoResponseDTO::sku, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(StockDisponibleComparativoResponseDTO::lote, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(StockDisponibleComparativoResponseDTO::ubicacion, Comparator.nullsLast(String::compareToIgnoreCase));

        return base.thenComparing(fallback);
    }

    private String texto(String value) {
        return value == null ? "" : value;
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDate parseFecha(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private record ComparativoKey(String sku, String lote, String ubicacion) {
    }

    private static class BaseComparativo {
        private final String sku;
        private final String nombre;
        private final String udm;
        private final String lote;
        private final LocalDate vence;
        private final String ubicacion;
        private BigDecimal cantidadFechaIndicada = BigDecimal.ZERO;
        private BigDecimal cantidadActual = BigDecimal.ZERO;

        private BaseComparativo(String sku,
                               String nombre,
                               String udm,
                               String lote,
                               LocalDate vence,
                               String ubicacion) {
            this.sku = sku;
            this.nombre = nombre;
            this.udm = udm;
            this.lote = lote;
            this.vence = vence;
            this.ubicacion = ubicacion;
        }

        private StockDisponibleComparativoResponseDTO toDto() {
            BigDecimal diferencia = cantidadActual.subtract(cantidadFechaIndicada);
            return new StockDisponibleComparativoResponseDTO(
                    sku,
                    nombre,
                    udm,
                    cantidadFechaIndicada,
                    cantidadActual,
                    diferencia,
                    lote,
                    vence,
                    ubicacion
            );
        }
    }
}
