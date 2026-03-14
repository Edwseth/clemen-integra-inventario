package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LotePendienteUbicarPtResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LotePendienteUbicarResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.LoteProductoMapper;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import com.willyes.clemenintegra.shared.util.DateParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<LoteProductoResponseDTO> crearLote(@RequestBody LoteProductoRequestDTO dto) {
        LoteProductoResponseDTO response = service.crearLote(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyAuthority('INV_LOTES_READ')")
    public ResponseEntity<List<LoteProductoResponseDTO>> listarPorEstado(@PathVariable String estado) {
        List<LoteProductoResponseDTO> lotes = service.obtenerLotesPorEstado(estado);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INV_LOTES_READ')")
    public ResponseEntity<Page<LoteProductoResponseDTO>> listar(
            @RequestParam(required = false) String producto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String almacen,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false, defaultValue = "false") Boolean vencidos,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @PageableDefault(size = 10, sort = "fechaFabricacion", direction = Sort.Direction.DESC) Pageable pageable) {

        // Validación básica de paginación
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }

        // Sanitiza sort y define default distinto si es "vencidos"
        Pageable sanitized = PaginationUtil.sanitize(
                pageable,
                java.util.List.of("fechaFabricacion", "fechaVencimiento", "id"),
                Boolean.TRUE.equals(vencidos) ? "fechaVencimiento" : "fechaFabricacion"
        );

        // Parse de estado (opcional)
        EstadoLote enumEstado = null;
        if (estado != null && !estado.isBlank()) {
            try {
                enumEstado = EstadoLote.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException ignore) {
                // estado inválido: se ignora el filtro
            }
        }

        LocalDateTime inicio = null;
        LocalDateTime fin = null;

        // ✅ Si ES consulta de vencidos, ignorar fechas (no son requeridas)
        if (!Boolean.TRUE.equals(vencidos)) {
            // Si llega UNO de los dos, exige ambos
            if ((fechaInicio == null) ^ (fechaFin == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren fechaInicio y fechaFin");
            }

            // Si llegan ambos, parsea y valida rango
            if (fechaInicio != null && fechaFin != null) {
                try {
                    inicio = DateParser.parseStart(fechaInicio);
                    fin = DateParser.parseEnd(fechaFin);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                }
                if (inicio.isAfter(fin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaInicio no puede ser mayor a fechaFin");
                }
            }
            // Si no llega ninguno, se listará sin filtro de fechas
        }

        Page<LoteProductoResponseDTO> lotes =
                service.listarTodos(producto, productoId, enumEstado, almacen, almacenId, vencidos, inicio, fin, sanitized);

        return ResponseEntity.ok(lotes);
    }

    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    @GetMapping("/por-evaluar")
    public ResponseEntity<Page<LoteProductoResponseDTO>> obtenerLotesPorEvaluar(
            @RequestParam(name = "estado", required = false) EstadoLote estado,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "fechaFabricacion",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<LoteProductoResponseDTO> resultado = service.obtenerLotesPorEvaluar(estado, pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}/evaluaciones")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<java.util.List<EvaluacionCalidadResponseDTO>> obtenerEvaluaciones(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.listarPorLote(id));
    }


    @GetMapping("/pendientes-ubicar")
    @PreAuthorize("hasAnyAuthority('INV_LOTES_READ','INV_READ')")
    public ResponseEntity<Page<LotePendienteUbicarResponseDTO>> listarPendientesUbicar(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        Pageable efectivo = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<LotePendienteUbicarResponseDTO> lotes = service.obtenerPendientesUbicar(efectivo);
        return ResponseEntity.ok(lotes);
    }


    @GetMapping("/pendientes-ubicar-pt")
    @PreAuthorize("hasAnyAuthority('INV_LOTES_READ','INV_READ')")
    public ResponseEntity<Page<LotePendienteUbicarPtResponseDTO>> listarPendientesUbicarPt(
            @PageableDefault(size = 10, sort = "fechaVencimiento", direction = Sort.Direction.ASC) Pageable pageable) {

        Pageable efectivo = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<LotePendienteUbicarPtResponseDTO> lotes = service.obtenerPendientesUbicarPt(efectivo);
        return ResponseEntity.ok(lotes);
    }

    @PutMapping("/{id}/liberar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_DECIDE','INV_WORKFLOW')")
    public ResponseEntity<LoteProductoResponseDTO> liberar(@PathVariable Long id,
                                                           @RequestBody(required = false) com.willyes.clemenintegra.inventario.dto.ObservacionRequestDTO request) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(service.liberarLote(id, observacion));
    }

    @PutMapping("/{id}/rechazar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_DECIDE','INV_WORKFLOW')")
    public ResponseEntity<LoteProductoResponseDTO> rechazar(@PathVariable Long id,
                                                            @RequestBody(required = false) com.willyes.clemenintegra.inventario.dto.ObservacionRequestDTO request) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(service.rechazarLote(id, observacion));
    }

    @PutMapping("/{id}/liberar-retenido")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_DECIDE','INV_WORKFLOW')")
    public ResponseEntity<LoteProductoResponseDTO> liberarRetenido(@PathVariable Long id,
                                                                   @RequestBody(required = false) com.willyes.clemenintegra.inventario.dto.ObservacionRequestDTO request) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(service.liberarLoteRetenido(id, observacion));
    }

}
