package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionCalidadController {

    private final EvaluacionCalidadService service;

    @GetMapping
    public ResponseEntity<Page<EvaluacionCalidadResponseDTO>> listar(
            @RequestParam(required = false) ResultadoEvaluacion resultado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(resultado, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EvaluacionCalidadResponseDTO> crear(@RequestBody EvaluacionCalidadRequestDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> actualizar(@PathVariable Long id,
                                                                   @RequestBody EvaluacionCalidadRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
