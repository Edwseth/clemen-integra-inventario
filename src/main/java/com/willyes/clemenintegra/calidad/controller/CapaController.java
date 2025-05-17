package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.service.CapaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/capas")
@RequiredArgsConstructor
public class CapaController {

    private final CapaService service;

    @GetMapping
    public ResponseEntity<List<CapaDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CapaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CapaDTO> crear(@RequestBody CapaDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CapaDTO> actualizar(@PathVariable Long id, @RequestBody CapaDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

