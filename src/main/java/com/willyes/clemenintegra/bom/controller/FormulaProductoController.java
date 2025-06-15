package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/formulas")
@RequiredArgsConstructor
public class FormulaProductoController {

    private final FormulaProductoService formulaService;

    @GetMapping
    public List<FormulaProductoResponse> listarTodas() {
        return formulaService.listarTodas().stream()
                .map(BomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaProductoResponse> obtenerPorId(@PathVariable Long id) {
        return formulaService.buscarPorId(id)
                .map(BomMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FormulaProductoResponse> crear(@RequestBody FormulaProductoRequest request) {
        Producto producto = new Producto(); producto.setId(request.productoId);
        Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
        FormulaProducto entidad = BomMapper.toEntity(request, producto, creador);
        return ResponseEntity.ok(BomMapper.toResponse(formulaService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormulaProductoResponse> actualizar(@PathVariable Long id, @RequestBody FormulaProductoRequest request) {
        return formulaService.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.productoId);
                    Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
                    FormulaProducto entidad = BomMapper.toEntity(request, producto, creador);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(BomMapper.toResponse(formulaService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        formulaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

