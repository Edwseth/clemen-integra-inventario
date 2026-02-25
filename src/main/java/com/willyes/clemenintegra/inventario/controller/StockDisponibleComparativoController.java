package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.StockDisponibleComparativoResponseDTO;
import com.willyes.clemenintegra.inventario.service.StockDisponibleComparativoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.util.List;

@RestController
@RequestMapping("/api/inventario/reportes/stock-disponible")
@RequiredArgsConstructor
@Slf4j
public class StockDisponibleComparativoController {

    private static final MediaType EXCEL = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StockDisponibleComparativoService stockDisponibleComparativoService;

    @GetMapping("/comparativo")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<List<StockDisponibleComparativoResponseDTO>> obtenerComparativo(
            @RequestParam(name = "fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "direccion", required = false) String direccion) {
        List<StockDisponibleComparativoResponseDTO> respuesta =
                stockDisponibleComparativoService.obtenerComparativo(fecha, categoriaId, orden, direccion);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping(value = "/comparativo/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportarComparativo(
            @RequestParam(name = "fecha")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "direccion", required = false) String direccion) {
        log.info("Generando reporte comparativo de stock disponible fecha={} categoriaId={}", fecha, categoriaId);
        try (Workbook workbook = stockDisponibleComparativoService.generarExcelComparativo(fecha, categoriaId, orden, direccion);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=stock_disponible_comparativo_" + fecha + ".xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Error generando reporte comparativo de stock disponible", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
