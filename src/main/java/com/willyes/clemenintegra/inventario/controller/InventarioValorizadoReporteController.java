package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.reportes.InventarioValorizadoRowDTO;
import com.willyes.clemenintegra.inventario.service.InventarioValorizadoReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/inventario/reportes")
@RequiredArgsConstructor
@Slf4j
public class InventarioValorizadoReporteController {

    private static final MediaType EXCEL = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final InventarioValorizadoReportService inventarioValorizadoReportService;

    @GetMapping("/inventario-valorizado")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<Page<InventarioValorizadoRowDTO>> listar(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(inventarioValorizadoReportService.listar(pageable));
    }

    @GetMapping(value = "/inventario-valorizado/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportarExcel() {
        try (Workbook workbook = inventarioValorizadoReportService.generarExcel();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventario_valorizado.xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Error generando reporte de inventario valorizado", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
