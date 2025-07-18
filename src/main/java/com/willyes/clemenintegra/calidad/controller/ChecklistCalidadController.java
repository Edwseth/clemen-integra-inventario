package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import com.willyes.clemenintegra.calidad.service.ChecklistCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/checklists")
@RequiredArgsConstructor
public class ChecklistCalidadController {

    private final ChecklistCalidadService service;

    @GetMapping
    public ResponseEntity<Page<ChecklistCalidadDTO>> listar(
            @RequestParam(required = false) TipoChecklist tipo,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(tipo, pageable));
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

