package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EspecificacionCalidadDTO;
import com.willyes.clemenintegra.calidad.service.EspecificacionCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/especificaciones")
@RequiredArgsConstructor
public class EspecificacionCalidadController {

    private final EspecificacionCalidadService service;

    @GetMapping
    public ResponseEntity<List<EspecificacionCalidadDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspecificacionCalidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EspecificacionCalidadDTO> crear(@RequestBody EspecificacionCalidadDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspecificacionCalidadDTO> actualizar(@PathVariable Long id,
                                                               @RequestBody EspecificacionCalidadDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

