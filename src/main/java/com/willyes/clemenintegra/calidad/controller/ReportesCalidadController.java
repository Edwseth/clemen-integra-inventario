package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import com.willyes.clemenintegra.calidad.service.CarpetaLotePdfService;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.ReporteInvimaBpmPdfService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Fachada de reportes de Calidad (INVIMA/BPM).
 * <ul>
 *     <li>Excel evaluaciones: {@code GET /api/calidad/reportes/evaluaciones/excel}</li>
 *     <li>Carpeta de lote: {@code GET /api/calidad/reportes/lote/{loteId}/carpeta-pdf}</li>
 *     <li>PDF análisis microbiológico: {@code GET /api/calidad/reportes/lote/{loteId}/analisis-micro-pdf}</li>
 *     <li>PDF INVIMA/BPM v1: {@code GET /api/calidad/reportes/invima-bpm/v1/pdf?loteId=}</li>
 * </ul>
 * Roles con acceso: Jefe de Calidad, Analista de Calidad, Microbiólogo y Super Admin.
 * La lógica de generación en servicios se considera estable y válida para auditoría/regulador.
 */
@RestController
@RequestMapping("/api/calidad/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
public class ReportesCalidadController {

    private final EvaluacionCalidadService evaluacionCalidadService;
    private final CarpetaLotePdfService carpetaLotePdfService;
    private final AuditoriaLoteService auditoriaLoteService;
    private final ResultadoAnalisisMicroService resultadoAnalisisMicroService;
    private final ReporteInvimaBpmPdfService reporteInvimaBpmPdfService;

    @GetMapping(path = "/evaluaciones/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    // Reporte: Excel consolidado de evaluaciones de calidad.
    public ResponseEntity<byte[]> exportarEvaluacionesExcel(@RequestParam(required = false) LocalDate fechaInicio,
                                                            @RequestParam(required = false) LocalDate fechaFin,
                                                            @RequestParam(required = false) ResultadoEvaluacion resultado) {
        byte[] excel = evaluacionCalidadService.generarReporteEvaluacionesExcel(fechaInicio, fechaFin, resultado);
        String nombreArchivo = "reporte-evaluaciones-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(excel);
    }

    @GetMapping(path = "/lote/{loteId}/carpeta-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF de carpeta de lote (expediente de calidad).
    public ResponseEntity<byte[]> descargarCarpetaLote(@PathVariable Long loteId) {
        byte[] pdf = carpetaLotePdfService.generarCarpeta(loteId);
        String nombreArchivo = "carpeta-lote-" + loteId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

    @GetMapping(path = "/lote/{loteId}/analisis-micro-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF de análisis microbiológico asociado al lote.
    public ResponseEntity<byte[]> descargarAnalisisMicro(@PathVariable Long loteId) {
        AuditoriaLoteResponseDTO auditoria = auditoriaLoteService.obtenerAuditoriaDeLote(loteId);
        if (auditoria.getEvaluaciones() == null || auditoria.getEvaluaciones().isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No hay evaluaciones con PDF microbiológico para el lote");
        }
        AuditoriaLoteResponseDTO.EvaluacionResumenDTO evaluacion = auditoria.getEvaluaciones().stream()
                .filter(AuditoriaLoteResponseDTO.EvaluacionResumenDTO::isPdfMicroDisponible)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No hay PDF microbiológico disponible para el lote"));
        byte[] pdf = resultadoAnalisisMicroService.obtenerPdfMicro(evaluacion.getId());
        String nombreArchivo = "analisis-micro-" + (auditoria.getCodigoLote() != null ? auditoria.getCodigoLote() : loteId) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

    @GetMapping(path = "/invima-bpm/v1/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF INVIMA/BPM v1 para auditoría/regulador.
    public ResponseEntity<byte[]> descargarInvimaBpm(@RequestParam Long loteId) {
        byte[] pdf = reporteInvimaBpmPdfService.generarPdf(loteId);
        String nombreArchivo = "invima-bpm-" + loteId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }
}
