package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.service.AuditoriaLotePdfService;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import com.willyes.clemenintegra.calidad.service.CarpetaLotePdfService;
import com.willyes.clemenintegra.calidad.service.ReporteInvimaBpmPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calidad")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
public class CalidadAuditoriaController {

    private final AuditoriaLoteService auditoriaLoteService;
    private final AuditoriaLotePdfService auditoriaLotePdfService;
    private final CarpetaLotePdfService carpetaLotePdfService;
    private final ReporteInvimaBpmPdfService reporteInvimaBpmPdfService;

    @GetMapping("/auditoria-lote/{loteId}")
    public ResponseEntity<AuditoriaLoteResponseDTO> obtenerAuditoriaLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(auditoriaLoteService.obtenerAuditoriaDeLote(loteId));
    }

    @GetMapping(path = "/auditoria-lote/{loteId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF de auditoría/carpeta de lote usado en inspecciones de Calidad.
    public ResponseEntity<byte[]> descargarPdfAuditoria(@PathVariable Long loteId) {
        AuditoriaLoteResponseDTO auditoria = auditoriaLoteService.obtenerAuditoriaDeLote(loteId);
        byte[] pdf = auditoriaLotePdfService.generarPdf(auditoria);
        String nombreArchivo = "auditoria-lote-" + (auditoria.getCodigoLote() != null ? auditoria.getCodigoLote() : loteId) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

    @GetMapping(path = "/reportes/lotes/{loteId}/carpeta", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF de carpeta de lote (expediente de calidad).
    public ResponseEntity<byte[]> descargarCarpetaLote(@PathVariable Long loteId) {
        byte[] pdf = carpetaLotePdfService.generarCarpeta(loteId);
        String nombreArchivo = "carpeta-lote-" + loteId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

    @GetMapping(path = "/reportes/invima-bpm/lote/{loteId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // Reporte: PDF INVIMA/BPM v1 para auditoría/regulador.
    public ResponseEntity<byte[]> descargarReporteInvimaBpm(@PathVariable Long loteId) {
        byte[] pdf = reporteInvimaBpmPdfService.generarPdf(loteId);
        String nombreArchivo = "invima-bpm-" + loteId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }
}
