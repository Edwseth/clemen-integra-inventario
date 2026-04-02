package com.willyes.clemenintegra.gerencial.controller;

import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.gerencial.service.SeguimientoGerencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gerencial/planes-semanales")
@RequiredArgsConstructor
/**
 * Endpoint de lectura para el seguimiento gerencial transversal de un plan semanal.
 *
 * <p>Ruta publicada:
 * {@code GET /api/gerencial/planes-semanales/{planSemanalId}/seguimiento}.
 *
 * <p>Propósito funcional:
 * entregar en una sola respuesta un consolidado de estado para consumo gerencial:
 * <ul>
 *   <li>{@code summary}: agregados del plan completo (totales y cumplimiento general).</li>
 *   <li>{@code items[]}: detalle item a item (1:1 con {@code plan_produccion_detalle}) con semántica operativa.</li>
 * </ul>
 */
public class SeguimientoGerencialController {

    private final SeguimientoGerencialService seguimientoGerencialService;

    /**
     * Obtiene el seguimiento gerencial V1 de un plan semanal existente.
     *
     * <p>La respuesta usa el DTO consolidado {@link SeguimientoGerencialResponseDTO}
     * con estructura {@code summary + items[]}.
     */
    @GetMapping("/{planSemanalId}/seguimiento")
    @PreAuthorize("hasAuthority('GER_SEGUIMIENTO_READ')")
    public ResponseEntity<SeguimientoGerencialResponseDTO> obtenerSeguimiento(@PathVariable Long planSemanalId) {
        return ResponseEntity.ok(seguimientoGerencialService.obtenerSeguimiento(planSemanalId));
    }
}
