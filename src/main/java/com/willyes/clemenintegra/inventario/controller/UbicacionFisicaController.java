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
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<UbicacionFisicaResponseDTO>> listar(
            @RequestParam Integer almacenId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ubicacionFisicaService.buscar(almacenId, q));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<UbicacionFisicaResponseDTO> crear(@Valid @RequestBody UbicacionFisicaRequestDTO dto) {
        UbicacionFisicaResponseDTO creada = ubicacionFisicaService.crear(dto);
        return ResponseEntity.status(201).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<UbicacionFisicaResponseDTO> actualizar(@PathVariable Long id,
                                                                 @Valid @RequestBody UbicacionFisicaRequestDTO dto) {
        return ResponseEntity.ok(ubicacionFisicaService.actualizar(id, dto));
    }
}
