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
    @PreAuthorize("hasAnyAuthority(" +
            "'ROL_JEFE_ALMACENES'," +
            "'ROL_ALMACENISTA'," +
            "'ROL_JEFE_PRODUCCION'," +
            "'ROL_LIDER_ALIMENTOS'," +
            "'ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD'," +
            "'ROL_ANALISTA_CALIDAD'," +
            "'ROL_MICROBIOLOGO'," +
            "'ROL_SUPER_ADMIN'" +
            ")")
    public ResponseEntity<AuditoriaLoteResponseDTO> obtenerAuditoriaLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(auditoriaLoteService.obtenerAuditoriaDeLote(loteId));
    }
}
