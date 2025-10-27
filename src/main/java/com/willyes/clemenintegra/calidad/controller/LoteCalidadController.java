package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EstadoCalidadLoteResponseDTO;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/lotes")
@RequiredArgsConstructor
public class LoteCalidadController {

    private final LoteProductoService service;

    @PutMapping("/{loteId}/liberar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<LoteProductoResponseDTO> liberarLote(
            @PathVariable Long loteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        return ResponseEntity.ok(service.liberarLotePorCalidad(loteId, usuario));
    }

    @GetMapping("/{loteId}/estado-calidad")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO')")
    public ResponseEntity<EstadoCalidadLoteResponseDTO> obtenerEstadoCalidad(@PathVariable Long loteId) {
        return ResponseEntity.ok(service.obtenerEstadoCalidad(loteId));
    }
}
