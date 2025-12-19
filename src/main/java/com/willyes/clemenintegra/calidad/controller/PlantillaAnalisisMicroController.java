package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calidad/plantillas-micro")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
public class PlantillaAnalisisMicroController {

    private final PlantillaAnalisisMicroService plantillaAnalisisMicroService;

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<PlantillaAnalisisMicroDTO> obtenerPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(plantillaAnalisisMicroService.obtenerPorProducto(productoId));
    }
}
