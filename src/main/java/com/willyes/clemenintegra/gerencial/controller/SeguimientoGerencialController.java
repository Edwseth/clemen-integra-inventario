package com.willyes.clemenintegra.gerencial.controller;

import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.gerencial.service.SeguimientoGerencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gerencial/planes-semanales")
@RequiredArgsConstructor
public class SeguimientoGerencialController {

    private final SeguimientoGerencialService seguimientoGerencialService;

    @GetMapping("/{planSemanalId}/seguimiento")
    public ResponseEntity<SeguimientoGerencialResponseDTO> obtenerSeguimiento(@PathVariable Long planSemanalId) {
        return ResponseEntity.ok(seguimientoGerencialService.obtenerSeguimiento(planSemanalId));
    }
}
