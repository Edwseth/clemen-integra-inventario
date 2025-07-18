package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.ItemChecklistDTO;
import com.willyes.clemenintegra.calidad.service.ItemChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/checklist-items")
@RequiredArgsConstructor
public class ItemChecklistController {

    private final ItemChecklistService service;

    @GetMapping
    public ResponseEntity<Page<ItemChecklistDTO>> listar(
            @RequestParam(required = false) Long checklistId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(checklistId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemChecklistDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ItemChecklistDTO> crear(@RequestBody ItemChecklistDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemChecklistDTO> actualizar(@PathVariable Long id, @RequestBody ItemChecklistDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

