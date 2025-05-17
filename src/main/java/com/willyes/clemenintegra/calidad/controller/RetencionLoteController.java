package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/retenciones")
@RequiredArgsConstructor
public class RetencionLoteController {

    private final RetencionLoteService service;

    @GetMapping
    public ResponseEntity<List<RetencionLoteDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetencionLoteDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<RetencionLoteDTO> crear(@RequestBody RetencionLoteDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RetencionLoteDTO> actualizar(@PathVariable Long id, @RequestBody RetencionLoteDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

