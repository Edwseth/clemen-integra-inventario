package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarpetaLotePdfService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuditoriaLoteService auditoriaLoteService;
    private final EvaluacionCalidadRepository evaluacionCalidadRepository;

    public byte[] generarCarpeta(Long loteId) {
        AuditoriaLoteResponseDTO auditoria = auditoriaLoteService.obtenerAuditoriaDeLote(loteId);
        List<EvaluacionCalidad> evaluaciones = evaluacionCalidadRepository.findByLoteProductoId(loteId);

        String template = loadTemplate("templates/calidad/carpeta-lote.ftl");
        template = template.replace("${fechaGeneracion}", FECHA_FORMATO.format(LocalDateTime.now()));
        template = template.replace("${codigoLote}", esc(auditoria.getCodigoLote()));
        template = template.replace("${producto}", esc(auditoria.getNombreProducto()));
        template = template.replace("${estadoLote}", esc(auditoria.getEstadoLote()));
        template = template.replace("${tablaEvaluaciones}", buildEvaluaciones(evaluaciones));
        template = template.replace("${tablaIncidentes}", buildIncidentes(auditoria));
        template = template.replace("${tablaMovimientos}", buildMovimientos(auditoria));

        return renderPdf(template);
    }

    private String buildEvaluaciones(List<EvaluacionCalidad> evaluaciones) {
        if (evaluaciones == null || evaluaciones.isEmpty()) {
            return "<tr><td colspan='5'>Sin evaluaciones registradas</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        evaluaciones.forEach(eval -> {
            String anexos = eval.getArchivosAdjuntos() == null ? "" : eval.getArchivosAdjuntos().stream()
                    .map(this::nombreAdjunto)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.joining(", "));
            sb.append("<tr>")
                    .append("<td>").append(formatDate(eval.getFechaEvaluacion())).append("</td>")
                    .append("<td>").append(eval.getTipoEvaluacion() != null ? esc(eval.getTipoEvaluacion().name()) : "").append("</td>")
                    .append("<td>").append(eval.getResultado() != null ? esc(eval.getResultado().name()) : "").append("</td>")
                    .append("<td>").append(esc(eval.getUsuarioEvaluador() != null ? eval.getUsuarioEvaluador().getNombreCompleto() : "")).append("</td>")
                    .append("<td>").append(esc(anexos)).append("</td>")
                    .append("</tr>");
        });
        return sb.toString();
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

    private String nombreAdjunto(ArchivoEvaluacion adjunto) {
        if (adjunto == null) {
            return "";
        }
        if (adjunto.getNombreVisible() != null && !adjunto.getNombreVisible().isBlank()) {
            return adjunto.getNombreVisible();
        }
        return adjunto.getNombreArchivo() != null ? adjunto.getNombreArchivo() : "";
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
            throw new IllegalStateException("No se pudo generar la carpeta de lote", e);
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
            throw new IllegalStateException("No se pudo cargar la plantilla de carpeta de lote", e);
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
