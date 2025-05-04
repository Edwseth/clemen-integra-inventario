package com.willyes.clemenintegra.inventario.api.controller;

import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaRequestDTO;
import com.willyes.clemenintegra.inventario.application.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.domain.service.UnidadMedidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    public ResponseEntity<List<UnidadMedidaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(unidadMedidaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadMedidaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(unidadMedidaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<UnidadMedidaResponseDTO> crear(@Valid @RequestBody UnidadMedidaRequestDTO dto) {
        UnidadMedidaResponseDTO creado = unidadMedidaService.crear(dto);
        return ResponseEntity.status(201).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadMedidaResponseDTO> actualizar(@PathVariable Long id,
                                                              @Valid @RequestBody UnidadMedidaRequestDTO dto) {
        UnidadMedidaResponseDTO actualizado = unidadMedidaService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        unidadMedidaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

