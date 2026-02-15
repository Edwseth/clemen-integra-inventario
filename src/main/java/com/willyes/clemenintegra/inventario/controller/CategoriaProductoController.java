package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.CategoriaProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaProductoController {

    private final CategoriaProductoService categoriaProductoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INV_CATEGORIAS_READ')")
    public ResponseEntity<List<CategoriaProductoResponseDTO>> listar() {
        return ResponseEntity.ok(categoriaProductoService.listarTodas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INV_CATEGORIAS_READ')")
    public ResponseEntity<CategoriaProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaProductoService.obtenerPorId(id));
    }

    @PostMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<CategoriaProductoResponseDTO> crear(@Valid @RequestBody CategoriaProductoRequestDTO dto) {
        var creado = categoriaProductoService.crear(dto);
        return ResponseEntity.status(201).body(creado);
    }

    @PutMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_WRITE','INV_WORKFLOW')")
    public ResponseEntity<CategoriaProductoResponseDTO> actualizar(@PathVariable Long id,
                                                                   @Valid @RequestBody CategoriaProductoRequestDTO dto) {
        var actualizado = categoriaProductoService.actualizar(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_DECIDE','INV_WORKFLOW')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaProductoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
