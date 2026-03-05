package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.reportes.ConteoAjusteReporteDTO;
import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/inventario/reportes")
@RequiredArgsConstructor
@Slf4j
public class InventarioReportesController {

    private static final MediaType EXCEL = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteInventarioService reporteInventarioService;

    @GetMapping("/conteos-ajuste")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<Page<ConteoAjusteReporteDTO>> listarConteosAjuste(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(name = "soloDiferencias", required = false) Boolean soloDiferencias,
            @RequestParam(name = "soloConDiferencia", required = false) Boolean soloConDiferencia,
            @PageableDefault(size = 20) Pageable pageable) {

        boolean aplicarSoloDiferencias = Boolean.TRUE.equals(soloDiferencias)
                || (soloDiferencias == null && Boolean.TRUE.equals(soloConDiferencia));

        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "fechaAplicacion")
        );

        Page<ConteoAjusteReporteDTO> page = reporteInventarioService.listarConteosAjuste(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX),
                almacenId,
                productoId,
                aplicarSoloDiferencias,
                pageRequest
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping(value = "/conteos-ajuste/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportarConteosAjusteExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(name = "soloDiferencias", required = false) Boolean soloDiferencias,
            @RequestParam(name = "soloConDiferencia", required = false) Boolean soloConDiferencia) {
        boolean aplicarSoloDiferencias = Boolean.TRUE.equals(soloDiferencias)
                || (soloDiferencias == null && Boolean.TRUE.equals(soloConDiferencia));
        try (Workbook workbook = reporteInventarioService.generarExcelConteosAjuste(
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX),
                almacenId,
                productoId,
                aplicarSoloDiferencias
        ); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=conteos_antes_despues_ajuste_" + LocalDateTime.now().toLocalDate() + ".xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Error generando reporte Excel de conteos antes y después del ajuste", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
