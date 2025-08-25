package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteInventarioController {

    private final ReporteInventarioService service;
    private final ProductoService productoService;
    private final LoteProductoService loteProductoService;
    private final MovimientoInventarioService movimientoService;

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

    @GetMapping("/stock-actual")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarStockActual() throws IOException {
        Workbook workbook = productoService.generarReporteStockActualExcel();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_actual.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
    }

    @GetMapping("/productos-por-vencer")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarLotesPorVencer() throws IOException {
        Workbook workbook = loteProductoService.generarReporteLotesPorVencerExcel();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotes_por_vencer.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bos.toByteArray());
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

    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarReporteMovimientos() throws IOException {
        byte[] contenido;
        try (Workbook workbook = movimientoService.generarReporteMovimientosExcel();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            contenido = bos.toByteArray();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_movimientos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }
}
