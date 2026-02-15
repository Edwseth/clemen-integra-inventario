package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.reportes.ReporteComprasRowDTO;
import com.willyes.clemenintegra.inventario.service.ReporteComprasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes/compras")
@RequiredArgsConstructor
public class ReporteComprasController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteComprasService reporteComprasService;

    @GetMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public List<ReporteComprasRowDTO> obtenerReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteComprasService.generar(desde, hasta);
    }

    @GetMapping("/excel")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] excel = reporteComprasService.exportarExcel(desde, hasta);
        String fileName = String.format("reporte_compras_%s_%s.xlsx", desde, hasta);

        return ResponseEntity.ok()
                .contentType(EXCEL_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(excel);
    }
}
