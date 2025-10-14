package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.service.LoteVencimientoJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventario/tasks")
@RequiredArgsConstructor
public class InventarioTaskController {

    private final LoteVencimientoJobService jobService;

    @PostMapping("/expirar-lotes")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_CALIDAD')")
    public ResponseEntity<?> expirarLotes(@RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun) {
        try {
            if (dryRun) {
                LoteVencimientoJobService.LoteVencimientoPreview preview = jobService.dryRun();
                return ResponseEntity.ok(Map.of(
                        "total", preview.total(),
                        "loteIds", preview.loteIds()
                ));
            }
            LoteVencimientoJobService.LoteVencimientoExecutionResult result = jobService.ejecutar();
            return ResponseEntity.ok(Map.of(
                    "totalActualizados", result.totalActualizados(),
                    "totalMovimientos", result.totalMovimientos()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "CONFIG_INCOMPLETA", "message", ex.getMessage()));
        }
    }
}
