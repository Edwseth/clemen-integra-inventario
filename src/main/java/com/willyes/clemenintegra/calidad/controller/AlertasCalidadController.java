package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.ResumenAlertasCalidadDTO;
import com.willyes.clemenintegra.calidad.service.AlertasCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calidad/alertas")
@RequiredArgsConstructor
public class AlertasCalidadController {

    private final AlertasCalidadService alertasCalidadService;

    @GetMapping("/resumen")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<ResumenAlertasCalidadDTO> obtenerResumen(@RequestParam(defaultValue = "30") int diasUmbral) {
        return ResponseEntity.ok(alertasCalidadService.obtenerAlertas(diasUmbral));
    }
}
