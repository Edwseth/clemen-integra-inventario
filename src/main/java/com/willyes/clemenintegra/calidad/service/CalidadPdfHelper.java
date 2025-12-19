package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

final class CalidadPdfHelper {

    private CalidadPdfHelper() {
    }

    static String buildEvaluaciones(List<EvaluacionCalidad> evaluaciones, DateTimeFormatter formato) {
        if (evaluaciones == null || evaluaciones.isEmpty()) {
            return "<tr><td colspan='5'>Sin evaluaciones registradas</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        evaluaciones.forEach(eval -> {
            String anexos = eval.getArchivosAdjuntos() == null ? "" : eval.getArchivosAdjuntos().stream()
                    .map(CalidadPdfHelper::nombreAdjunto)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.joining(", "));
            sb.append("<tr>")
                    .append("<td>").append(formatDate(eval.getFechaEvaluacion(), formato)).append("</td>")
                    .append("<td>").append(eval.getTipoEvaluacion() != null ? esc(eval.getTipoEvaluacion().name()) : "").append("</td>")
                    .append("<td>").append(eval.getResultado() != null ? esc(eval.getResultado().name()) : "").append("</td>")
                    .append("<td>").append(esc(eval.getUsuarioEvaluador() != null ? eval.getUsuarioEvaluador().getNombreCompleto() : "")).append("</td>")
                    .append("<td>").append(esc(anexos)).append("</td>")
                    .append("</tr>");
        });
        return sb.toString();
    }

    static String buildIncidentes(AuditoriaLoteResponseDTO auditoria, DateTimeFormatter formato) {
        if (auditoria.getIncidentes() == null || auditoria.getIncidentes().isEmpty()) {
            return "<tr><td colspan='7'>Sin incidentes asociados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getIncidentes().forEach(nc -> sb.append("<tr>")
                .append("<td>").append(esc(nc.getCodigo())).append("</td>")
                .append("<td>").append(nc.getTipoIncidente() != null ? nc.getTipoIncidente().name() : "").append("</td>")
                .append("<td>").append(nc.getSeveridad() != null ? nc.getSeveridad().name() : "").append("</td>")
                .append("<td>").append(nc.getEstado() != null ? nc.getEstado().name() : "").append("</td>")
                .append("<td>").append(formatDate(nc.getFechaApertura(), formato)).append("</td>")
                .append("<td>").append(formatDate(nc.getFechaCierre(), formato)).append("</td>")
                .append("<td>").append(nc.isTieneCapa() ? "SI" : "NO").append("</td>")
                .append("</tr>"));
        return sb.toString();
    }

    static String buildMovimientos(AuditoriaLoteResponseDTO auditoria, DateTimeFormatter formato) {
        if (auditoria.getMovimientos() == null || auditoria.getMovimientos().isEmpty()) {
            return "<tr><td colspan='9'>Sin movimientos registrados</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        auditoria.getMovimientos().forEach(mov -> sb.append("<tr>")
                .append("<td>").append(formatDate(mov.getFechaMovimiento(), formato)).append("</td>")
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

    static String nombreAdjunto(ArchivoEvaluacion adjunto) {
        if (adjunto == null) {
            return "";
        }
        if (adjunto.getNombreVisible() != null && !adjunto.getNombreVisible().isBlank()) {
            return adjunto.getNombreVisible();
        }
        return adjunto.getNombreArchivo() != null ? adjunto.getNombreArchivo() : "";
    }

    static String esc(String val) {
        return val == null ? "" : val
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    static String formatDate(LocalDateTime fecha, DateTimeFormatter formato) {
        return fecha == null ? "" : fecha.format(formato);
    }
}
