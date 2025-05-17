package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/no-conformidades")
@RequiredArgsConstructor
public class NoConformidadController {

    private final NoConformidadService service;

    @GetMapping
    public ResponseEntity<List<NoConformidadDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoConformidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<NoConformidadDTO> crear(@RequestBody NoConformidadDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoConformidadDTO> actualizar(@PathVariable Long id, @RequestBody NoConformidadDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

