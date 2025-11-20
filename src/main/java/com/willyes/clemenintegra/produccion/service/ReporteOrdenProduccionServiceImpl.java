package com.willyes.clemenintegra.produccion.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.service.ReporteOrdenProduccionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class ReporteOrdenProduccionServiceImpl implements ReporteOrdenProduccionService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] generarExcelOrdenesProduccion(List<OrdenProduccion> ordenes) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("ordenes");
            CreationHelper creationHelper = workbook.getCreationHelper();

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            String[] headers = {
                    "Código", "Producto", "Lote producción", "Responsable", "Estado",
                    "Cantidad programada", "Cantidad producida", "% Cumplimiento",
                    "Fecha inicio", "Fecha fin", "Fecha cierre"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (OrdenProduccion orden : ordenes) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                row.createCell(col++).setCellValue(nullSafe(orden.getCodigoOrden()));
                row.createCell(col++).setCellValue(orden.getProducto() != null ? nullSafe(orden.getProducto().getNombre()) : "");
                row.createCell(col++).setCellValue(nullSafe(orden.getLoteProduccion()));
                row.createCell(col++).setCellValue(obtenerResponsable(orden));
                row.createCell(col++).setCellValue(orden.getEstado() != null ? orden.getEstado().name() : "");
                setNumeric(row.createCell(col++), orden.getCantidadProgramada());
                setNumeric(row.createCell(col++), orden.getCantidadProducidaAcumulada());
                setNumeric(row.createCell(col++), calcularPorcentaje(orden));
                setDate(row.createCell(col++), orden.getFechaInicio(), dateStyle);
                setDate(row.createCell(col++), orden.getFechaFin(), dateStyle);
                setDate(row.createCell(col), orden.getFechaCierre(), dateStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de órdenes de producción", e);
            throw new IllegalStateException("No se pudo generar el Excel de órdenes de producción", e);
        }
    }

    @Override
    public byte[] generarPdfOrdenesProduccion(List<OrdenProduccion> ordenes) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = construirHtml(ordenes);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de órdenes de producción", e);
            throw new IllegalStateException("No se pudo generar el PDF de órdenes de producción", e);
        }
    }

    private void setNumeric(Cell cell, java.math.BigDecimal value) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private void setDate(Cell cell, LocalDateTime value, CellStyle dateStyle) {
        if (value != null) {
            cell.setCellValue(java.sql.Timestamp.valueOf(value));
            cell.setCellStyle(dateStyle);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String obtenerResponsable(OrdenProduccion orden) {
        if (orden.getResponsable() == null) {
            return "";
        }
        String nombreCompleto = orden.getResponsable().getNombreCompleto();
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            return nombreCompleto;
        }
        return nullSafe(orden.getResponsable().getNombreUsuario());
    }

    private java.math.BigDecimal calcularPorcentaje(OrdenProduccion orden) {
        if (orden.getPorcentajeCumplimiento() != null) {
            return orden.getPorcentajeCumplimiento();
        }
        if (orden.getCantidadProgramada() == null || orden.getCantidadProgramada().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return null;
        }
        java.math.BigDecimal acumulada = Objects.requireNonNullElse(orden.getCantidadProducidaAcumulada(), java.math.BigDecimal.ZERO);
        return acumulada.multiply(java.math.BigDecimal.valueOf(100))
                .divide(orden.getCantidadProgramada(), 2, java.math.RoundingMode.HALF_UP);
    }

    private String construirHtml(List<OrdenProduccion> ordenes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>")
                .append("table { width: 100%; border-collapse: collapse; font-family: Arial, sans-serif; font-size: 12px; }")
                .append("th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }")
                .append("th { background-color: #f2f2f2; }")
                .append("</style></head><body>")
                .append("<h2>Órdenes de Producción</h2>")
                .append("<table><thead><tr>")
                .append("<th>Código</th>")
                .append("<th>Producto</th>")
                .append("<th>Lote producción</th>")
                .append("<th>Responsable</th>")
                .append("<th>Estado</th>")
                .append("<th>Cant. programada</th>")
                .append("<th>Cant. producida</th>")
                .append("<th>% Cumplimiento</th>")
                .append("<th>Fecha inicio</th>")
                .append("<th>Fecha fin</th>")
                .append("<th>Fecha cierre</th>")
                .append("</tr></thead><tbody>");

        for (OrdenProduccion orden : ordenes) {
            sb.append("<tr>")
                    .append(td(orden.getCodigoOrden()))
                    .append(td(orden.getProducto() != null ? orden.getProducto().getNombre() : ""))
                    .append(td(orden.getLoteProduccion()))
                    .append(td(obtenerResponsable(orden)))
                    .append(td(orden.getEstado() != null ? orden.getEstado().name() : ""))
                    .append(td(formatDecimal(orden.getCantidadProgramada())))
                    .append(td(formatDecimal(orden.getCantidadProducidaAcumulada())))
                    .append(td(formatDecimal(calcularPorcentaje(orden))))
                    .append(td(formatDate(orden.getFechaInicio())))
                    .append(td(formatDate(orden.getFechaFin())))
                    .append(td(formatDate(orden.getFechaCierre())))
                    .append("</tr>");
        }

        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }

    private String td(String value) {
        return "<td>" + escape(value) + "</td>";
    }

    private String formatDecimal(java.math.BigDecimal value) {
        if (value == null) return "";
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
