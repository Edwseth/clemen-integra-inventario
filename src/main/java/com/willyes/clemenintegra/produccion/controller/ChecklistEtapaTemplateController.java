package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateCopyRequest;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateRequest;
import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaTemplateResponse;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaTemplateAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class ChecklistEtapaTemplateController {

    private final ChecklistEtapaTemplateAdminService service;

    // TODO(rbac-prod-cut2): retirar fallback por roles legacy al finalizar migracion canónica.

    @GetMapping("/plantillas-etapas/{etapaPlantillaId}/checklist")
    @PreAuthorize("hasAnyAuthority('PROD_READ')")
    public List<ChecklistEtapaTemplateResponse> listar(@PathVariable Long etapaPlantillaId) {
        return service.listar(etapaPlantillaId);
    }

    @PostMapping("/plantillas-etapas/{etapaPlantillaId}/checklist")
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<ChecklistEtapaTemplateResponse> crear(@PathVariable Long etapaPlantillaId,
                                                                @RequestBody ChecklistEtapaTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(etapaPlantillaId, request));
    }

    @PutMapping("/checklist-template/{id}")
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<ChecklistEtapaTemplateResponse> actualizar(@PathVariable Long id,
                                                                     @RequestBody ChecklistEtapaTemplateRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/checklist-template/{id}")
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plantillas-etapas/{etapaPlantillaId}/checklist/copiar-desde")
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<List<ChecklistEtapaTemplateResponse>> copiar(@PathVariable Long etapaPlantillaId,
                                                                       @RequestBody ChecklistEtapaTemplateCopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.copiarDesde(etapaPlantillaId, request.getOrigenEtapaPlantillaId()));
    }
}
