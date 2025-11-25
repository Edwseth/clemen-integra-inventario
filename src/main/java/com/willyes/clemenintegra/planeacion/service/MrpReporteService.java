package com.willyes.clemenintegra.planeacion.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MrpReporteService {

    private static final Locale ES_CO = new Locale("es", "CO");

    private final CorridaMrpRepository corridaMrpRepository;

    @Transactional(readOnly = true)
    public byte[] generarExcelCorrida(Long corridaId) {
        CorridaMrp corrida = cargarCorrida(corridaId);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MRP");
            int rowIdx = 0;

            Row titulo = sheet.createRow(rowIdx++);
            titulo.createCell(0).setCellValue("Resumen de Corrida MRP");

            Row filaNumero = sheet.createRow(rowIdx++);
            filaNumero.createCell(0).setCellValue("Número de corrida");
            filaNumero.createCell(1).setCellValue(Optional.ofNullable(corrida.getId()).orElse(0L));

            Row filaPlan = sheet.createRow(rowIdx++);
            filaPlan.createCell(0).setCellValue("Plan semanal");
            filaPlan.createCell(1).setCellValue(corrida.getPlanProduccionSemanal() != null ?
                    String.valueOf(corrida.getPlanProduccionSemanal().getId()) : "");

            Row filaFechas = sheet.createRow(rowIdx++);
            filaFechas.createCell(0).setCellValue("Rango de fechas");
            filaFechas.createCell(1).setCellValue(formatearFecha(corrida.getHorizonteInicio()) + " - " + formatearFecha(corrida.getHorizonteFin()));

            Row filaEjecucion = sheet.createRow(rowIdx++);
            filaEjecucion.createCell(0).setCellValue("Fecha de ejecución");
            filaEjecucion.createCell(1).setCellValue(formatearFechaHora(corrida.getFechaEjecucion()));

            rowIdx++;
            Row encabezado = sheet.createRow(rowIdx++);
            String[] columnas = new String[]{
                    "Código insumo", "Nombre", "Categoría", "Requerimiento bruto",
                    "Inventario disponible", "Requerimiento neto", "Tipo sugerencia"
            };
            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
            }

            for (DetalleCorridaMrp detalle : Optional.ofNullable(corrida.getDetalles()).orElse(List.of())) {
                Row fila = sheet.createRow(rowIdx++);
                Producto producto = detalle.getProducto();
                CategoriaProducto categoria = producto != null ? producto.getCategoriaProducto() : null;
                SugerenciaAbastecimiento sugerencia = detalle.getSugerencia();

                int col = 0;
                fila.createCell(col++).setCellValue(producto != null ? nz(producto.getCodigoSku()) : "");
                fila.createCell(col++).setCellValue(producto != null ? nz(producto.getNombre()) : "");
                fila.createCell(col++).setCellValue(categoria != null ? nz(categoria.getNombre()) : "");
                setNumeric(fila.createCell(col++), detalle.getRequerimientoBruto());
                setNumeric(fila.createCell(col++), detalle.getInventarioDisponible());
                setNumeric(fila.createCell(col++), detalle.getRequerimientoNeto());
                fila.createCell(col).setCellValue(formatearTipoSugerencia(sugerencia));
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel de la corrida MRP", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfCorrida(Long corridaId) {
        CorridaMrp corrida = cargarCorrida(corridaId);
        String html = componerHtml(corrida);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la corrida MRP", e);
        }
    }

    private CorridaMrp cargarCorrida(Long corridaId) {
        return corridaMrpRepository.findWithDetallesById(corridaId)
                .orElseThrow(() -> new NoSuchElementException("Corrida MRP no encontrada"));
    }

    private String componerHtml(CorridaMrp corrida) {
        String template = loadTemplate("templates/mrp/mrp-resumen.ftl");
        template = template.replace("${numeroCorrida}", Optional.ofNullable(corrida.getId()).map(String::valueOf).orElse("-"));
        template = template.replace("${planId}", corrida.getPlanProduccionSemanal() != null ? String.valueOf(corrida.getPlanProduccionSemanal().getId()) : "-");
        template = template.replace("${fechaInicio}", formatearFecha(corrida.getHorizonteInicio()));
        template = template.replace("${fechaFin}", formatearFecha(corrida.getHorizonteFin()));
        template = template.replace("${fechaEjecucion}", formatearFechaHora(corrida.getFechaEjecucion()));

        StringBuilder filas = new StringBuilder();
        for (DetalleCorridaMrp detalle : Optional.ofNullable(corrida.getDetalles()).orElse(List.of())) {
            Producto producto = detalle.getProducto();
            CategoriaProducto categoria = producto != null ? producto.getCategoriaProducto() : null;
            SugerenciaAbastecimiento sugerencia = detalle.getSugerencia();

            filas.append("<tr>")
                    .append("<td>").append(esc(producto != null ? producto.getCodigoSku() : "")).append("</td>")
                    .append("<td>").append(esc(producto != null ? producto.getNombre() : "")).append("</td>")
                    .append("<td>").append(esc(categoria != null ? categoria.getNombre() : "")).append("</td>")
                    .append("<td class='right'>").append(formNum(detalle.getRequerimientoBruto())).append("</td>")
                    .append("<td class='right'>").append(formNum(detalle.getInventarioDisponible())).append("</td>")
                    .append("<td class='right'>").append(formNum(detalle.getRequerimientoNeto())).append("</td>")
                    .append("<td>").append(esc(formatearTipoSugerencia(sugerencia))).append("</td>")
                    .append("</tr>");
        }

        return template.replace("${detalleRows}", filas.toString());
    }

    private void setNumeric(Cell cell, BigDecimal value) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? fecha.toString() : "-";
    }

    private String formatearFechaHora(LocalDateTime fecha) {
        return fecha != null ? fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
    }

    private String formatearTipoSugerencia(SugerenciaAbastecimiento sugerencia) {
        if (sugerencia == null || sugerencia.getTipo() == null) {
            return "NINGUNA";
        }
        return sugerencia.getTipo() == TipoSugerenciaAbastecimiento.FABRICAR ? "OP" : "OC";
    }

    private String loadTemplate(String classpathLocation) {
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se encontró la plantilla: " + classpathLocation, e);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String formNum(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(ES_CO);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(Optional.ofNullable(v).orElse(BigDecimal.ZERO));
    }
}
