package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.shared.exception.PdfGenerationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditoriaLotePdfService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdf(AuditoriaLoteResponseDTO auditoria) {
        String template = loadTemplate("templates/calidad/auditoria-lote.ftl");

        template = template.replace("${fechaGeneracion}", FECHA_FORMATO.format(LocalDateTime.now()));
        template = template.replace("${codigoLote}", esc(auditoria.getCodigoLote()));
        template = template.replace("${producto}", esc(auditoria.getNombreProducto()));
        template = template.replace("${categoria}", esc(auditoria.getCategoriaProducto()));
        template = template.replace("${tipoAnalisis}", esc(auditoria.getTipoAnalisisCalidad()));
        template = template.replace("${estadoLote}", esc(auditoria.getEstadoLote()));
        template = template.replace("${fechaFabricacion}", formatDate(auditoria.getFechaFabricacion()));
        template = template.replace("${fechaVencimiento}", formatDate(auditoria.getFechaVencimiento()));
        template = template.replace("${stockLote}", auditoria.getStockLote() != null ? auditoria.getStockLote().toString() : "");
        template = template.replace("${almacen}", esc(auditoria.getNombreAlmacenActual()));
        template = template.replace("${ubicacion}", esc(auditoria.getUbicacionAlmacenActual()));

        template = template.replace("${estadoCalidad}", auditoria.getEstadoCalidad() != null
                ? esc(auditoria.getEstadoCalidad().getEstadoLote()) : "");
        template = template.replace("${retencionActiva}", auditoria.getEstadoCalidad() != null && auditoria.getEstadoCalidad().isRetencionActiva() ? "SI" : "NO");
        template = template.replace("${motivoRetencion}", auditoria.getEstadoCalidad() != null && auditoria.getEstadoCalidad().getMotivoRetencion() != null
                ? esc(auditoria.getEstadoCalidad().getMotivoRetencion().name()) : "");

        template = template.replace("${tablaIncidentes}", buildIncidentes(auditoria));
        template = template.replace("${tablaMovimientos}", buildMovimientos(auditoria));

        return renderPdf(template);
    }

    private String buildIncidentes(AuditoriaLoteResponseDTO auditoria) {
        if (auditoria.getIncidentes() == null || auditoria.getIncidentes().isEmpty()) {
            return "<tr><td colspan='7'>Sin incidentes asociados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getIncidentes().forEach(nc -> sb.append("<tr>")
                .append("<td>").append(esc(nc.getCodigo())).append("</td>")
                .append("<td>").append(nc.getTipoIncidente() != null ? nc.getTipoIncidente().name() : "").append("</td>")
                .append("<td>").append(nc.getSeveridad() != null ? nc.getSeveridad().name() : "").append("</td>")
                .append("<td>").append(nc.getEstado() != null ? nc.getEstado().name() : "").append("</td>")
                .append("<td>").append(formatDate(nc.getFechaApertura())).append("</td>")
                .append("<td>").append(formatDate(nc.getFechaCierre())).append("</td>")
                .append("<td>").append(nc.isTieneCapa() ? "SI" : "NO").append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    private String buildMovimientos(AuditoriaLoteResponseDTO auditoria) {
        if (auditoria.getMovimientos() == null || auditoria.getMovimientos().isEmpty()) {
            return "<tr><td colspan='9'>Sin movimientos registrados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getMovimientos().forEach(mov -> sb.append("<tr>")
                .append("<td>").append(formatDate(mov.getFechaMovimiento())).append("</td>")
                .append("<td>").append(mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : "").append("</td>")
                .append("<td>").append(mov.getClasificacion() != null ? mov.getClasificacion().name() : "").append("</td>")
                .append("<td>").append(mov.getCantidad() != null ? mov.getCantidad() : "").append("</td>")
                .append("<td>").append(esc(mov.getAlmacenOrigenNombre())).append("</td>")
                .append("<td>").append(esc(mov.getAlmacenDestinoNombre())).append("</td>")
                .append("<td>").append(esc(mov.getMotivoMovimientoNombre())).append("</td>")
                .append("<td>").append(esc(mov.getRegistradoPorNombre())).append("</td>")
                .append("<td>").append(esc(mov.getOrdenProduccionCodigo())).append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("No se pudo generar el PDF de auditoría de lote", e);
        }
    }

    private String loadTemplate(String path) {
        try {
            var resource = new ClassPathResource(path);
            try (var in = resource.getInputStream()) {
                String html = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                int index = html.indexOf('<');
                return index > 0 ? html.substring(index) : html;
            }
        } catch (Exception e) {
            throw new PdfGenerationException("No se pudo cargar la plantilla de auditoría de lote", e);
        }
    }

    private String esc(String val) {
        return val == null ? "" : val
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String formatDate(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_FORMATO);
    }
}
