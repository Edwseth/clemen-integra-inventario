package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.PicklistPtCreateRequest;
import com.willyes.clemenintegra.inventario.dto.PicklistPtResponse;
import com.willyes.clemenintegra.inventario.dto.PicklistPtResumenDTO;
import com.willyes.clemenintegra.inventario.model.enums.PicklistPtEstado;
import com.willyes.clemenintegra.inventario.service.PicklistPtService;
import com.willyes.clemenintegra.shared.util.DateParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/inventario/picklists-pt")
@RequiredArgsConstructor
public class PicklistPtController {

    private final PicklistPtService picklistPtService;

    @PostMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<PicklistPtResponse> crear(@Valid @RequestBody PicklistPtCreateRequest request) {
        return ResponseEntity.ok(picklistPtService.crear(request));
    }

    @GetMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<Page<PicklistPtResumenDTO>> listar(
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) PicklistPtEstado estado,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta
    ) {
        LocalDateTime inicio = null;
        LocalDateTime fin = null;
        if ((fechaDesde != null && fechaHasta == null) || (fechaDesde == null && fechaHasta != null)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Se requieren fechaDesde y fechaHasta");
        }
        if (fechaDesde != null) {
            inicio = DateParser.parseStart(fechaDesde);
            fin = DateParser.parseEnd(fechaHasta);
            if (inicio.isAfter(fin)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "fechaDesde no puede ser mayor a fechaHasta");
            }
        }
        return ResponseEntity.ok(picklistPtService.listar(estado, cliente, inicio, fin, pageable));
    }

    @GetMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<PicklistPtResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.obtener(id));
    }

    @GetMapping("/{id}/pdf")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_EXPORT')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = picklistPtService.generarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=picklist-pt-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{id}/confirmar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<PicklistPtResponse> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.confirmar(id));
    }

    @PostMapping("/{id}/ejecutar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<PicklistPtResponse> ejecutar(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.ejecutar(id));
    }

    @PostMapping("/{id}/cancelar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WORKFLOW','INV_DECIDE')")
    public ResponseEntity<PicklistPtResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.cancelar(id));
    }
}
