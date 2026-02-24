package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.ReporteInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.InventarioGeneralCorteReportService;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.dto.InventarioGeneralPreviewRowDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Slf4j
public class ReporteInventarioController {

    private final ReporteInventarioService service;
    private final ProductoService productoService;
    private final LoteProductoService loteProductoService;
    private final MovimientoInventarioService movimientoService;
    private final InventarioGeneralCorteReportService inventarioGeneralCorteReportService;
    private final ProductoRepository productoRepository;

    private static final MediaType EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping("/alta-rotacion")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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

    @GetMapping(value = "/productos-vencidos",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportProductosVencidos(
            @RequestParam(name = "producto", required = false) Long productoId,
            @RequestParam(name = "almacen", required = false) Long almacenId) {
        try (Workbook wb = service.generarReporteProductosVencidosExcel(productoId, almacenId);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            wb.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("productos_vencidos.xlsx").build());
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el Excel de productos vencidos", e);
        }
    }

    @GetMapping("/alertas-inventario")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportarAlertasInventario() {
        ByteArrayOutputStream stream = loteProductoService.generarReporteAlertasActivasExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertas_activas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(stream.toByteArray());
    }


    // Paginación in-memory sobre las mismas filas del Excel; con ~10 usuarios concurrentes se prioriza consistencia funcional.
    @GetMapping("/inventario-general/preview")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<Page<InventarioGeneralPreviewRowDTO>> previewInventarioGeneralCorte(
            @RequestParam(name = "fechaCorte")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
            @RequestParam(required = false) Long categoriaProductoId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LocalDateTime hasta = fechaCorte.atTime(23, 59, 59);
        Set<Long> productIds = resolverProductoIds(categoriaProductoId);
        if (categoriaProductoId != null && productIds.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        List<InventarioGeneralPreviewRowDTO> filas = inventarioGeneralCorteReportService
                .calcularFilasInventarioGeneralCorte(hasta, productIds)
                .stream()
                .map(f -> new InventarioGeneralPreviewRowDTO(
                        f.sku(),
                        f.nombre(),
                        f.udm(),
                        f.cant(),
                        f.lote(),
                        f.vence(),
                        f.ubicacion()
                ))
                .toList();

        List<InventarioGeneralPreviewRowDTO> ordenadas = aplicarOrdenPreview(filas, pageable);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ordenadas.size());
        List<InventarioGeneralPreviewRowDTO> content = start >= ordenadas.size() ? List.of() : ordenadas.subList(start, end);
        Page<InventarioGeneralPreviewRowDTO> page = new PageImpl<>(content, pageable, ordenadas.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping(value = "/inventario-general", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
    public ResponseEntity<byte[]> exportarInventarioGeneralCorte(
            @RequestParam(name = "fechaCorte", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
            @RequestParam(required = false) Long categoriaProductoId
    ) {
        if (fechaCorte == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"fechaCorte es obligatoria (YYYY-MM-DD)\"}".getBytes());
        }

        LocalDateTime hasta = fechaCorte.atTime(23, 59, 59);
        Set<Long> productIds = resolverProductoIds(categoriaProductoId);

        try (Workbook workbook = inventarioGeneralCorteReportService
                .generarExcelInventarioGeneralCorte(hasta, productIds);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(EXCEL);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=inventario_general_corte_" + fechaCorte + ".xlsx");
            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.warn("Validación fallida al generar reporte de inventario general al corte", ex);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"fechaCorte es obligatoria (YYYY-MM-DD)\"}".getBytes());
        } catch (Exception ex) {
            log.error("Error generando reporte de inventario general al corte", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    private Set<Long> resolverProductoIds(Long categoriaProductoId) {
        if (categoriaProductoId == null) {
            return null;
        }
        return Set.copyOf(productoRepository.findIdsByCategoriaProductoId(categoriaProductoId));
    }

    private List<InventarioGeneralPreviewRowDTO> aplicarOrdenPreview(List<InventarioGeneralPreviewRowDTO> filas, Pageable pageable) {
        Comparator<InventarioGeneralPreviewRowDTO> comparator = null;

        if (pageable.getSort().isSorted()) {
            for (var order : pageable.getSort()) {
                Comparator<InventarioGeneralPreviewRowDTO> current = comparatorPorCampo(order.getProperty());
                if (current == null) {
                    continue;
                }
                Comparator<InventarioGeneralPreviewRowDTO> currentOrdenado = order.isAscending() ? current : current.reversed();
                comparator = comparator == null ? currentOrdenado : comparator.thenComparing(currentOrdenado);
            }
        }

        Comparator<InventarioGeneralPreviewRowDTO> fallbackNombreAsc = Comparator
                .comparing(InventarioGeneralPreviewRowDTO::nombre, Comparator.nullsLast(String::compareToIgnoreCase));

        Comparator<InventarioGeneralPreviewRowDTO> comparadorFinal = comparator == null
                ? fallbackNombreAsc
                : comparator.thenComparing(fallbackNombreAsc);

        return filas.stream().sorted(comparadorFinal).toList();
    }

    private Comparator<InventarioGeneralPreviewRowDTO> comparatorPorCampo(String property) {
        String normalizedProperty = property == null ? "" : property.trim().toLowerCase();
        return switch (normalizedProperty) {
            case "sku" -> Comparator.comparing(row -> normalizarTexto(row.sku()), Comparator.nullsLast(String::compareTo));
            case "nombre" -> Comparator.comparing(InventarioGeneralPreviewRowDTO::nombre, Comparator.nullsLast(String::compareToIgnoreCase));
            case "udm" -> Comparator.comparing(InventarioGeneralPreviewRowDTO::udm, Comparator.nullsLast(String::compareToIgnoreCase));
            case "cant" -> Comparator.comparing(InventarioGeneralPreviewRowDTO::cant, Comparator.nullsLast(BigDecimal::compareTo));
            case "lote" -> Comparator.comparing(InventarioGeneralPreviewRowDTO::lote, Comparator.nullsLast(String::compareToIgnoreCase));
            case "vence" -> Comparator.comparing(this::parseVence, Comparator.nullsLast(LocalDate::compareTo));
            case "ubicacion" -> Comparator.comparing(InventarioGeneralPreviewRowDTO::ubicacion, Comparator.nullsLast(String::compareToIgnoreCase));
            default -> null;
        };
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.toUpperCase(Locale.ROOT);
    }

    private LocalDate parseVence(InventarioGeneralPreviewRowDTO row) {
        if (row.vence() == null || row.vence().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(row.vence());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    @GetMapping(value = "/movimientos", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('INV_EXPORT','INV_REPORTES_EXPORT')")
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
