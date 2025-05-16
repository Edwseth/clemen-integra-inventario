package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.service.BitacoraCambiosInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/bitacora")
@RequiredArgsConstructor
public class BitacoraCambiosInventarioController {

    private final BitacoraCambiosInventarioService service;

    @GetMapping
    public ResponseEntity<List<BitacoraCambiosInventarioDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<BitacoraCambiosInventarioDTO> crear(@RequestBody BitacoraCambiosInventarioDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

