package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import com.willyes.clemenintegra.shared.util.DateParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteProductoController {

    private final LoteProductoService service;
    private final LoteProductoRepository loteProductoRepository;
    private final LoteProductoMapper mapper;
    private final EvaluacionCalidadService evaluacionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<LoteProductoResponseDTO> crearLote(@RequestBody LoteProductoRequestDTO dto) {
        LoteProductoResponseDTO response = service.crearLote(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<LoteProductoResponseDTO>> listarPorEstado(@PathVariable String estado) {
        List<LoteProductoResponseDTO> lotes = service.obtenerLotesPorEstado(estado);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/reporte-vencimiento")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ANALISTA_CALIDAD', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarLotesPorVencer() throws IOException {
        Workbook workbook = service.generarReporteLotesPorVencerExcel();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lotes_por_vencer.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(bos.toByteArray());
    }

    @GetMapping("/reporte-alertas")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ANALISTA_CALIDAD', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarAlertasActivas() {
        ByteArrayOutputStream stream = service.generarReporteAlertasActivasExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alertas_activas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(stream.toByteArray());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<LoteProductoResponseDTO>> listar(
            @RequestParam(required = false) String producto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String almacen,
            @RequestParam(required = false) Boolean vencidos,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @PageableDefault(size = 10, sort = "fechaFabricacion", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        if (fechaInicio == null || fechaFin == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Se requieren fechaInicio y fechaFin"));
        }
        try {
            LocalDateTime inicio = DateParser.parseStart(fechaInicio);
            LocalDateTime fin = DateParser.parseEnd(fechaFin);
            if (inicio.isAfter(fin)) {
                return ResponseEntity.badRequest().body(Map.of("message", "fechaInicio no puede ser mayor a fechaFin"));
            }
            Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaFabricacion", "fechaVencimiento", "id"), "fechaFabricacion");
            EstadoLote enumEstado = null;
            if (estado != null && !estado.isBlank()) {
                try {
                    enumEstado = EstadoLote.valueOf(estado.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    // ignorar filtro inválido
                }
            }
            Page<LoteProductoResponseDTO> lotes = service.listarTodos(producto, enumEstado, almacen, vencidos, inicio, fin, sanitized);
            return ResponseEntity.ok(lotes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_SUPER_ADMIN')")
    @GetMapping("/por-evaluar")
    public ResponseEntity<List<LoteProductoResponseDTO>> obtenerLotesPorEvaluar() {
        List<LoteProductoResponseDTO> resultado = service.obtenerLotesPorEvaluar();

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}/evaluaciones")
    @PreAuthorize("hasAuthority('ROL_JEFE_CALIDAD')")
    public ResponseEntity<java.util.List<EvaluacionCalidadResponseDTO>> obtenerEvaluaciones(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.listarPorLote(id));
    }

    @PutMapping("/{id}/liberar")
    @PreAuthorize("hasAuthority('ROL_JEFE_CALIDAD')")
    public ResponseEntity<LoteProductoResponseDTO> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(service.liberarLote(id));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('ROL_JEFE_CALIDAD')")
    public ResponseEntity<LoteProductoResponseDTO> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(service.rechazarLote(id));
    }

    @PutMapping("/{id}/liberar-retenido")
    @PreAuthorize("hasAuthority('ROL_JEFE_CALIDAD')")
    public ResponseEntity<LoteProductoResponseDTO> liberarRetenido(@PathVariable Long id) {
        return ResponseEntity.ok(service.liberarLoteRetenido(id));
    }

}

