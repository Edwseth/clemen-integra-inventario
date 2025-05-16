package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;
import com.willyes.clemenintegra.inventario.service.TipoMovimientoDetalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/tipos-movimiento-detalle")
@RequiredArgsConstructor
public class TipoMovimientoDetalleController {

    private final TipoMovimientoDetalleService service;

    @GetMapping
    public ResponseEntity<List<TipoMovimientoDetalleDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    public ResponseEntity<TipoMovimientoDetalleDTO> crear(@RequestBody TipoMovimientoDetalleDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
