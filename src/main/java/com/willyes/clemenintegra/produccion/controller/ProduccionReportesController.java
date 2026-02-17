package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.ConsumoTeoricoRealResponseDTO;
import com.willyes.clemenintegra.produccion.service.ConsumoTeoricoRealService;
import com.willyes.clemenintegra.produccion.service.ReportesProduccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produccion/ordenes")
@RequiredArgsConstructor
public class ProduccionReportesController {

    private final ConsumoTeoricoRealService consumoTeoricoRealService;
    private final ReportesProduccionService reportesProduccionService;

    @GetMapping("/{id}/consumo-real-vs-teorico")
    @PreAuthorize("hasAnyAuthority('PROD_REPORTS_READ')")
    public ResponseEntity<ConsumoTeoricoRealResponseDTO> consumoRealVsTeorico(@PathVariable Long id) {
        return ResponseEntity.ok(consumoTeoricoRealService.obtenerConsumo(id));
    }

    @GetMapping(value = "/{id}/batch-record/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_EXPORT')")
    public ResponseEntity<byte[]> descargarBatchRecordExcel(@PathVariable Long id) {
        byte[] excel = reportesProduccionService.generarBatchRecordExcel(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=batch-record-" + id + ".xlsx");
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/batch-record/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_EXPORT')")
    public ResponseEntity<byte[]> descargarBatchRecordPdf(@PathVariable Long id) {
        byte[] pdf = reportesProduccionService.generarBatchRecordPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=batch-record-" + id + ".pdf");
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
