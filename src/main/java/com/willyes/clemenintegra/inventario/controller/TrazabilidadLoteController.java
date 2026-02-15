package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.calidad.dto.AuditoriaLoteResponseDTO;
import com.willyes.clemenintegra.calidad.service.AuditoriaLoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventarios/trazabilidad")
@RequiredArgsConstructor
public class TrazabilidadLoteController {

    private final AuditoriaLoteService auditoriaLoteService;

    @GetMapping("/lote/{loteId}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<AuditoriaLoteResponseDTO> obtenerAuditoriaLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(auditoriaLoteService.obtenerAuditoriaDeLote(loteId));
    }
}
