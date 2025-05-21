package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> obtenerTodos(
            @RequestParam(required = false) String categoria) {
        if (categoria != null) {
            return ResponseEntity.ok(productoService.buscarPorCategoria(categoria));
        }
        return ResponseEntity.ok(productoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoRequestDTO dto) {
        try {
            ProductoResponseDTO creado = productoService.crearProducto(dto);
            return ResponseEntity.status(201).body(creado);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("SKU duplicado o restricción de integridad violada");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // para debugging en consola
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        ProductoResponseDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequestDTO dto) {
        ProductoResponseDTO actualizado = productoService.actualizarProducto(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}

