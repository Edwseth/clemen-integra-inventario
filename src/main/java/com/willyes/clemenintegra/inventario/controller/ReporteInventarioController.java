package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Slf4j
public class ReporteInventarioController {

    private final ReporteInventarioService service;
    private final ProductoService productoService;
    private final LoteProductoService loteProductoService;
    private final MovimientoInventarioService movimientoService;

    private static final MediaType EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping("/alta-rotacion")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> altaRotacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) throws IOException {
        Workbook workbook = service.generarReporteAltaRotacion(fechaInicio, fechaFin);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alta_rotacion.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/baja-rotacion")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> bajaRotacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) throws IOException {
        Workbook workbook = service.generarReporteBajaRotacion(fechaInicio, fechaFin);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=baja_rotacion.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/mas-costosos")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> productosMasCostosos(
            @RequestParam(required = false) String categoria
    ) throws IOException {
        Workbook workbook = service.generarReporteProductosMasCostosos(categoria);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos_mas_costosos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/trazabilidad-lote")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_ANALISTA_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarTrazabilidadPorLote(@RequestParam String codigoLote) throws IOException {
        Workbook workbook = service.generarReporteTrazabilidadLote(codigoLote);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trazabilidad_lote.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/productos-retencion-liberacion")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarProductosRetencionLiberacion(
            @RequestParam(required = false) String estadoLote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) throws IOException {
        Workbook workbook = service.generarReporteProductosRetencionLiberacion(estadoLote, desde, hasta);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=retencion_liberacion.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/no-conformidades")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarNoConformidades(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) throws IOException {
        Workbook workbook = service.generarReporteNoConformidades(tipo, area, desde, hasta);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=no_conformidades.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/capas")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarCapas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) throws IOException {
        Workbook workbook = service.generarReporteCapas(estado, desde, hasta);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=capas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping(value = "/stock-disponible", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarStockDisponible() {
        log.info("Generando reporte de stock disponible");
        try (Workbook workbook = productoService.generarReporteStockDisponibleExcel();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_disponible.xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.warn("Validación fallida al generar reporte de stock disponible", ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Error generando reporte de stock disponible", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/productos-por-vencer", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarLotesPorVencer(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        log.info("Generando reporte de productos por vencer: inicio={}, fin={}", inicio, fin);
        try (Workbook workbook = loteProductoService.generarReporteLotesPorVencerExcel(inicio, fin);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotes_por_vencer.xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.warn("Validación fallida al generar reporte de productos por vencer", ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Error generando reporte de productos por vencer", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/alertas-inventario")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarAlertasInventario() {
        ByteArrayOutputStream stream = loteProductoService.generarReporteAlertasActivasExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertas_activas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(stream.toByteArray());
    }

    @GetMapping(value = "/movimientos", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarReporteMovimientos(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        log.info("Generando reporte de movimientos: inicio={}, fin={}", inicio, fin);
        try (Workbook workbook = movimientoService.generarReporteMovimientosExcel(inicio, fin);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_movimientos.xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.warn("Validación fallida al generar reporte de movimientos", ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Error generando reporte de movimientos", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
