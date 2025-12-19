package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
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
        template = template.replace("${tablaEvaluaciones}", CalidadPdfHelper.buildEvaluaciones(evaluaciones, FECHA_FORMATO));
        template = template.replace("${tablaIncidentes}", CalidadPdfHelper.buildIncidentes(auditoria, FECHA_FORMATO));
        template = template.replace("${tablaMovimientos}", CalidadPdfHelper.buildMovimientos(auditoria, FECHA_FORMATO));

        return renderPdf(template);
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
        return CalidadPdfHelper.esc(val);
    }

    private String formatDate(LocalDateTime fecha) {
        return CalidadPdfHelper.formatDate(fecha, FECHA_FORMATO);
    }
}
