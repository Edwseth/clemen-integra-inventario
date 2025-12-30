package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.ChecklistEtapaDTO;
import com.willyes.clemenintegra.produccion.dto.ChecklistItemDTO;
import com.willyes.clemenintegra.produccion.service.ChecklistEtapaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produccion/etapas")
@RequiredArgsConstructor
public class ChecklistEtapaController {

    private final ChecklistEtapaService checklistEtapaService;

    @GetMapping("/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> obtener(@PathVariable Long etapaId) {
        return ResponseEntity.ok(checklistEtapaService.obtenerPorEtapa(etapaId));
    }

    @PutMapping("/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> actualizar(@PathVariable Long etapaId,
                                                        @RequestBody List<ChecklistItemDTO> items) {
        return ResponseEntity.ok(checklistEtapaService.actualizar(etapaId, items));
    }

    @GetMapping(value = "/orden/{ordenId}/checklist/export", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportar(@PathVariable Long ordenId) {
        byte[] csv = checklistEtapaService.exportCsvPorOrden(ordenId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"checklist-op-" + ordenId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
