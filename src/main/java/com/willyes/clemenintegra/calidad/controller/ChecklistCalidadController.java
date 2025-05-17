package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.service.ChecklistCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/checklists")
@RequiredArgsConstructor
public class ChecklistCalidadController {

    private final ChecklistCalidadService service;

    @GetMapping
    public ResponseEntity<List<ChecklistCalidadDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChecklistCalidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ChecklistCalidadDTO> crear(@RequestBody ChecklistCalidadDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChecklistCalidadDTO> actualizar(@PathVariable Long id, @RequestBody ChecklistCalidadDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

