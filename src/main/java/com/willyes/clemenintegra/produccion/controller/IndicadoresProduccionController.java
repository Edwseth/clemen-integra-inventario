package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionIndicadoresService;
import com.willyes.clemenintegra.produccion.service.ReporteIndicadoresProduccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class IndicadoresProduccionController {

    private final ProduccionIndicadoresService produccionIndicadoresService;
    private final ReporteIndicadoresProduccionService reporteIndicadoresProduccionService;

    // TODO(rbac-prod-cut2): retirar fallback por roles y permisos granulares PROD_* legacy al finalizar migracion canónica.

    @GetMapping("/indicadores")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_INDICADORES_READ')")
    public ResponseEntity<IndicadoresProduccionResponseDTO> obtenerIndicadores(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "3") Integer diasAlerta) {
        return ResponseEntity.ok(produccionIndicadoresService.calcularIndicadores(fechaInicio, fechaFin, diasAlerta));
    }

    @GetMapping("/ordenes/alertas")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_ALERTAS_READ')")
    public ResponseEntity<List<AlertaOrdenProduccionDTO>> obtenerAlertas(
            @RequestParam(required = false) LocalDate fechaReferencia,
            @RequestParam(required = false, defaultValue = "3") Integer diasVentana) {
        return ResponseEntity.ok(produccionIndicadoresService.obtenerOrdenesConAlertas(fechaReferencia, diasVentana));
    }

    @GetMapping(value = "/indicadores/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('PROD_EXPORT','PROD_INDICADORES_EXPORT')")
    public ResponseEntity<byte[]> exportarIndicadoresExcel(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "3") Integer diasAlerta) {
        IndicadoresProduccionResponseDTO indicadores = produccionIndicadoresService
                .calcularIndicadores(fechaInicio, fechaFin, diasAlerta);
        byte[] excel = reporteIndicadoresProduccionService.generarExcelIndicadores(indicadores, fechaInicio, fechaFin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=indicadores-produccion.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(excel);
    }
}
