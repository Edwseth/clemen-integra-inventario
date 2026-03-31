package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteRequestDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.AjusteValorizacionLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.valorizacion.ValorizacionElegibilidadResponseDTO;
import com.willyes.clemenintegra.inventario.service.AjusteValorizacionLoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario/lotes")
@RequiredArgsConstructor
public class AjusteValorizacionLoteController {

    private final AjusteValorizacionLoteService service;

    @GetMapping("/{loteId}/valorizacion/elegibilidad")
    @PreAuthorize("hasAnyAuthority('INV_COSTEO_AJUSTE_READ','INV_COSTEO_AJUSTE_WRITE','INV_COSTEO_AJUSTE_OVERRIDE')")
    public ResponseEntity<ValorizacionElegibilidadResponseDTO> elegibilidad(@PathVariable Long loteId) {
        return ResponseEntity.ok(service.evaluarElegibilidad(loteId));
    }

    @PostMapping("/{loteId}/ajuste-valorizacion")
    @PreAuthorize("hasAnyAuthority('INV_COSTEO_AJUSTE_WRITE','INV_COSTEO_AJUSTE_OVERRIDE')")
    public ResponseEntity<AjusteValorizacionLoteResponseDTO> ajustar(@PathVariable Long loteId,
                                                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                                     @RequestBody @Valid AjusteValorizacionLoteRequestDTO request) {
        return ResponseEntity.ok(service.ajustar(loteId, request, idempotencyKey));
    }
}
