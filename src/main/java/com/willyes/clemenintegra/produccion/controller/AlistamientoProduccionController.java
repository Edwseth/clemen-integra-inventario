package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionAlistamientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produccion/ordenes")
@RequiredArgsConstructor
public class AlistamientoProduccionController {

    private final ProduccionAlistamientoService alistamientoService;

    @GetMapping("/{id}/alistamiento")
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_READ')")
    public ResponseEntity<AlistamientoOrdenProduccionDTO> obtenerAlistamiento(@PathVariable Long id) {
        return ResponseEntity.ok(alistamientoService.obtenerAlistamientoPorOrden(id));
    }
}
