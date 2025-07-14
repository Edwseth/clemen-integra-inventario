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
    private final BomMapper bomMapper;

    @GetMapping
    public List<FormulaProductoResponse> listarTodas() {
        return formulaService.listarTodas().stream()
                .map(formula -> bomMapper.toResponse(formula))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaProductoResponse> obtenerPorId(@PathVariable Long id) {
        return formulaService.buscarPorId(id)
                .map(formula -> bomMapper.toResponse(formula))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FormulaProductoResponse> crear(@RequestBody FormulaProductoRequest request) {
        Producto producto = new Producto(); producto.setId(request.productoId.intValue());
        Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
        FormulaProducto entidad = bomMapper.toEntity(request, producto, creador);
        return ResponseEntity.ok(bomMapper.toResponse(formulaService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormulaProductoResponse> actualizar(@PathVariable Long id, @RequestBody FormulaProductoRequest request) {
        return formulaService.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.productoId.intValue());
                    Usuario creador = new Usuario(); creador.setId(request.creadoPorId);
                    FormulaProducto entidad = bomMapper.toEntity(request, producto, creador);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(bomMapper.toResponse(formulaService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        formulaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

