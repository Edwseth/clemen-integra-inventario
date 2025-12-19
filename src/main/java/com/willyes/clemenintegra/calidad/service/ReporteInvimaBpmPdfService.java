package com.willyes.clemenintegra.calidad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadResumenDTO;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
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
public class ReporteInvimaBpmPdfService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuditoriaLoteService auditoriaLoteService;
    private final DocumentoCalidadService documentoCalidadService;
    private final LoteProductoRepository loteProductoRepository;

    public byte[] generarPdf(Long loteId) {
        AuditoriaLoteResponseDTO auditoria = auditoriaLoteService.obtenerAuditoriaDeLote(loteId);
        LoteProducto lote = loteProductoRepository.findById(loteId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO"));

        List<DocumentoCalidadResumenDTO> documentosLote = documentoCalidadService.listarVigentesPorLote(loteId);
        List<DocumentoCalidadResumenDTO> documentosSanitizacion = documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.SANITIZACION);
        List<DocumentoCalidadResumenDTO> documentosCalibracion = documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.CALIBRACION);
        List<DocumentoCalidadResumenDTO> documentosOtros = documentoCalidadService.listarVigentesPorTipo(DocumentoCalidadTipo.OTRO);

        if (documentosLote == null || documentosLote.isEmpty()) {
            documentosLote = combinar(documentosSanitizacion, documentosCalibracion, documentosOtros);
        }

        String template = loadTemplate("templates/calidad/invima-bpm-v1.ftl");
        template = template.replace("${fechaGeneracion}", FECHA_FORMATO.format(LocalDateTime.now()));
        template = template.replace("${codigoLote}", CalidadPdfHelper.esc(auditoria.getCodigoLote()));
        template = template.replace("${producto}", CalidadPdfHelper.esc(auditoria.getNombreProducto()));
        template = template.replace("${categoria}", CalidadPdfHelper.esc(auditoria.getCategoriaProducto()));
        template = template.replace("${almacen}", CalidadPdfHelper.esc(auditoria.getNombreAlmacenActual()));
        template = template.replace("${ubicacion}", CalidadPdfHelper.esc(auditoria.getUbicacionAlmacenActual()));
        template = template.replace("${proveedor}", "Sin proveedor asociado");

        template = template.replace("${estadoLiberacion}", CalidadPdfHelper.esc(auditoria.getEstadoLote()));
        template = template.replace("${fechaLiberacion}", CalidadPdfHelper.formatDate(lote.getFechaLiberacion(), FECHA_FORMATO));
        template = template.replace("${usuarioLiberador}", CalidadPdfHelper.esc(lote.getUsuarioLiberador() != null
                ? lote.getUsuarioLiberador().getNombreCompleto() : "Sin usuario liberador"));

        template = template.replace("${tablaEvaluaciones}", buildResumenEvaluaciones(auditoria));
        template = template.replace("${tablaIncidentes}", CalidadPdfHelper.buildIncidentes(auditoria, FECHA_FORMATO));
        template = template.replace("${tablaRetenciones}", buildRetenciones(auditoria));
        template = template.replace("${tablaCondiciones}", buildCondiciones(auditoria));
        template = template.replace("${tablaMovimientos}", CalidadPdfHelper.buildMovimientos(auditoria, FECHA_FORMATO));

        template = template.replace("${tablaDocumentos}", buildDocumentos(documentosLote));
        template = template.replace("${tablaSanitizacion}", buildDocumentos(documentosSanitizacion));
        template = template.replace("${tablaCalibracion}", buildDocumentos(documentosCalibracion));

        return renderPdf(template);
    }

    private String buildResumenEvaluaciones(AuditoriaLoteResponseDTO auditoria) {
        if (auditoria.getEvaluaciones() == null || auditoria.getEvaluaciones().isEmpty()) {
            return "<tr><td colspan='6'>Sin evaluaciones registradas</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getEvaluaciones().forEach(eval -> sb.append("<tr>")
                .append("<td>").append(eval.getTipoEvaluacion() != null ? eval.getTipoEvaluacion().name() : "").append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(eval.getResultado())).append("</td>")
                .append("<td>").append(CalidadPdfHelper.formatDate(eval.getFechaEvaluacion(), FECHA_FORMATO)).append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(eval.getUsuarioEvaluador())).append("</td>")
                .append("<td>").append(eval.isTieneResultadosMicro() ? "SI" : "NO").append("</td>")
                .append("<td>").append(eval.getConformeMicro() == null ? "N/A" : (eval.getConformeMicro() ? "CONFORME" : "NO CONFORME")).append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    private String buildRetenciones(AuditoriaLoteResponseDTO auditoria) {
        if (auditoria.getRetenciones() == null || auditoria.getRetenciones().isEmpty()) {
            return "<tr><td colspan='4'>Sin retenciones registradas</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getRetenciones().forEach(ret -> sb.append("<tr>")
                .append("<td>").append(ret.getMotivo() != null ? ret.getMotivo().name() : "").append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(ret.getDescripcion())).append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(ret.getEstado())).append("</td>")
                .append("<td>").append(ret.getId() != null ? ret.getId() : "").append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    private String buildCondiciones(AuditoriaLoteResponseDTO auditoria) {
        if (auditoria.getCondicionUsoActiva() == null) {
            return "<tr><td colspan='3'>Sin condiciones de uso activas</td></tr>";
        }
        AuditoriaLoteResponseDTO.CondicionUsoDTO condicion = auditoria.getCondicionUsoActiva();
        return "<tr><td>" + (condicion.getTipo() != null ? condicion.getTipo().name() : "")
                + "</td><td>" + CalidadPdfHelper.formatDate(condicion.getParametroFecha(), FECHA_FORMATO)
                + "</td><td>" + CalidadPdfHelper.esc(condicion.getDescripcion()) + "</td></tr>";
    }

    private String buildDocumentos(List<DocumentoCalidadResumenDTO> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return "<tr><td colspan='5'>Sin registros cargados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        documentos.forEach(doc -> sb.append("<tr>")
                .append("<td>").append(doc.getTipo() != null ? doc.getTipo().name() : "").append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(doc.getCodigo())).append("</td>")
                .append("<td>").append(CalidadPdfHelper.esc(doc.getNombre())).append("</td>")
                .append("<td>").append(doc.getVersion() != null ? doc.getVersion() : "").append("</td>")
                .append("<td>").append(CalidadPdfHelper.formatDate(doc.getFechaVersion(), FECHA_FORMATO)).append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    private List<DocumentoCalidadResumenDTO> combinar(List<DocumentoCalidadResumenDTO>... listas) {
        List<DocumentoCalidadResumenDTO> combinadas = new java.util.ArrayList<>();
        if (listas != null) {
            for (List<DocumentoCalidadResumenDTO> lista : listas) {
                if (lista != null) {
                    combinadas.addAll(lista);
                }
            }
        }
        return combinadas;
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
            throw new IllegalStateException("No se pudo generar el PDF INVIMA/BPM", e);
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
            throw new IllegalStateException("No se pudo cargar la plantilla INVIMA/BPM", e);
        }
    }
}
