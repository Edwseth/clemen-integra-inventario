package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.CategoriaProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaProductoController {

    private final CategoriaProductoService categoriaProductoService;

    @GetMapping
    public ResponseEntity<List<CategoriaProductoResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaProductoService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaProductoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<CategoriaProductoResponseDTO> crear(@Valid @RequestBody CategoriaProductoRequestDTO dto) {
        var creado = categoriaProductoService.crear(dto);
        return ResponseEntity.status(201).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProductoResponseDTO> actualizar(@PathVariable Long id,
                                                                   @Valid @RequestBody CategoriaProductoRequestDTO dto) {
        var actualizado = categoriaProductoService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaProductoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
