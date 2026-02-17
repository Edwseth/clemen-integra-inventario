package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/retenciones")
@RequiredArgsConstructor
// TODO(rbac-qc-cut3): retirar fallback por ROL_* cuando todos los perfiles usen permisos QC_* de forma canonica.
public class RetencionLoteController {

    private final RetencionLoteService service;
    private final RetencionLoteMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('QC_READ','QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<Page<RetencionLoteDTO>> listar(
            @RequestParam(required = false) EstadoRetencion estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('QC_READ','QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<RetencionLoteDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<RetencionLoteDTO> crear(
            @RequestBody RetencionLoteDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            dto.setAprobadoPorId(userDetails.getId());
        }
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<RetencionLoteDTO> actualizar(
            @PathVariable Long id,
            @RequestBody RetencionLoteDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            dto.setAprobadoPorId(userDetails.getId());
        }
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/levantar")
    @PreAuthorize("hasAnyAuthority('QC_WORKFLOW_FINISH','QC_DECIDE')")
    public ResponseEntity<RetencionLoteDTO> levantarRetencion(
            @PathVariable Long id,
            @RequestBody(required = false) com.willyes.clemenintegra.calidad.dto.LevantarRetencionRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RetencionLote retencion = service.levantar(id,
                userDetails != null ? new com.willyes.clemenintegra.shared.model.Usuario(userDetails.getId()) : null,
                request != null ? request.getObservacion() : null);
        return ResponseEntity.ok(mapper.toDTO(retencion));
    }
}
