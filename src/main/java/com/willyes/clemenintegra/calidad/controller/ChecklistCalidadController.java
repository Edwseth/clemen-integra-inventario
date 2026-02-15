package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import com.willyes.clemenintegra.calidad.service.ChecklistCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/checklists")
@RequiredArgsConstructor
// TODO(rbac-qc-cut3): retirar fallback por ROL_* cuando todos los perfiles usen permisos QC_* de forma canonica.
public class ChecklistCalidadController {

    private final ChecklistCalidadService service;

    @PreAuthorize("hasAnyAuthority('QC_READ','QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ChecklistCalidadDTO>> listar(
            @RequestParam(required = false) TipoChecklist tipo,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(tipo, pageable));
    }

    @PreAuthorize("hasAnyAuthority('QC_READ','QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ChecklistCalidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<ChecklistCalidadDTO> crear(@RequestBody ChecklistCalidadDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ChecklistCalidadDTO> actualizar(@PathVariable Long id, @RequestBody ChecklistCalidadDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @PreAuthorize("hasAnyAuthority('QC_WRITE','QC_WORKFLOW','QC_WORKFLOW_FINISH','QC_DECIDE','ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

