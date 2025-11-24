package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.dto.MrpSimpleRequestDTO;
import com.willyes.clemenintegra.planeacion.dto.SugerenciaAbastecimientoResponseDTO;
import com.willyes.clemenintegra.planeacion.service.PlaneacionMrpService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/planeacion/mrp")
@RequiredArgsConstructor
public class PlaneacionMrpController {

    private final PlaneacionMrpService planeacionMrpService;

    @PostMapping("/simple")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<CorridaMrpResponseDTO> ejecutarMrpSimple(@RequestBody MrpSimpleRequestDTO request,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails != null ? userDetails.getId() : null;
        CorridaMrpResponseDTO response = planeacionMrpService.ejecutarMrpSimple(request, usuarioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/corridas")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<CorridaMrpResponseDTO>> listarCorridas(
            @RequestParam(name = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(name = "modo", required = false) String modo,
            @PageableDefault(size = 20, sort = "fechaEjecucion") Pageable pageable) {
        Page<CorridaMrpResponseDTO> page = planeacionMrpService.listarCorridas(fechaDesde, fechaHasta, modo, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/corridas/{id}/sugerencias")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<SugerenciaAbastecimientoResponseDTO>> listarSugerenciasPorCorrida(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "fechaRequerida") Pageable pageable) {
        Page<SugerenciaAbastecimientoResponseDTO> page = planeacionMrpService.listarSugerenciasPorCorrida(id, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/sugerencias")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<SugerenciaAbastecimientoResponseDTO>> listarSugerencias(
            @RequestParam(name = "corridaId", required = false) Long corridaId,
            @RequestParam(name = "productoId", required = false) Long productoId,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "tipo", required = false) String tipo,
            @PageableDefault(size = 20, sort = "fechaRequerida") Pageable pageable) {
        Page<SugerenciaAbastecimientoResponseDTO> page = planeacionMrpService.listarSugerencias(corridaId, productoId, estado, tipo, pageable);
        return ResponseEntity.ok(page);
    }
}
