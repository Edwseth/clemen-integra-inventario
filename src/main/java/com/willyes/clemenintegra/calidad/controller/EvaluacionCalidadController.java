package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDTO;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionCalidadController {

    private final EvaluacionCalidadService service;

    @GetMapping
    public ResponseEntity<List<EvaluacionCalidadDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EvaluacionCalidadDTO> crear(@RequestBody EvaluacionCalidadDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadDTO> actualizar(@PathVariable Long id,
                                                           @RequestBody EvaluacionCalidadDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

