package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.PicklistPtCreateRequest;
import com.willyes.clemenintegra.inventario.dto.PicklistPtResponse;
import com.willyes.clemenintegra.inventario.service.PicklistPtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario/salida-pt/picklist")
@RequiredArgsConstructor
public class SalidaPtPicklistController {

    private final PicklistPtService picklistPtService;

    @PostMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<PicklistPtResponse> generar(@Valid @RequestBody PicklistPtCreateRequest request) {
        return ResponseEntity.ok(picklistPtService.crear(request));
    }

    @GetMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<PicklistPtResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.obtener(id));
    }

    @GetMapping("/{id}/pdf")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = picklistPtService.generarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=picklist-pt-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{id}/ejecutar")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<PicklistPtResponse> ejecutar(@PathVariable Long id) {
        return ResponseEntity.ok(picklistPtService.ejecutar(id));
    }
}
