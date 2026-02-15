package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaRequestDTO;
import com.willyes.clemenintegra.inventario.dto.UbicacionFisicaResponseDTO;
import com.willyes.clemenintegra.inventario.service.UbicacionFisicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
public class UbicacionFisicaController {

    private final UbicacionFisicaService ubicacionFisicaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INV_UBICACIONES_READ')")
    public ResponseEntity<List<UbicacionFisicaResponseDTO>> listar(
            @RequestParam Integer almacenId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ubicacionFisicaService.buscar(almacenId, q));
    }

    @PostMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<UbicacionFisicaResponseDTO> crear(@Valid @RequestBody UbicacionFisicaRequestDTO dto) {
        UbicacionFisicaResponseDTO creada = ubicacionFisicaService.crear(dto);
        return ResponseEntity.status(201).body(creada);
    }

    @PutMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<UbicacionFisicaResponseDTO> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody UbicacionFisicaRequestDTO dto) {
        return ResponseEntity.ok(ubicacionFisicaService.actualizar(id, dto));
    }
}
