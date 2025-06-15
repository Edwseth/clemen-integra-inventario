package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/detalles")
@RequiredArgsConstructor
public class DetalleFormulaController {

    private final DetalleFormulaService detalleService;

    @GetMapping
    public List<DetalleFormulaResponse> listarTodas() {
        return detalleService.listarTodas().stream()
                .map(BomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleFormulaResponse> obtenerPorId(@PathVariable Long id) {
        return detalleService.buscarPorId(id)
                .map(BomMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DetalleFormulaResponse> crear(@RequestBody DetalleFormulaRequest request) {
        FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
        Producto insumo = new Producto(); insumo.setId(request.insumoId);
        UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
        DetalleFormula entidad = BomMapper.toEntity(request, formula, insumo, unidad);
        return ResponseEntity.ok(BomMapper.toResponse(detalleService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleFormulaResponse> actualizar(@PathVariable Long id, @RequestBody DetalleFormulaRequest request) {
        return detalleService.buscarPorId(id)
                .map(existente -> {
                    FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
                    Producto insumo = new Producto(); insumo.setId(request.insumoId);
                    UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
                    DetalleFormula entidad = BomMapper.toEntity(request, formula, insumo, unidad);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(BomMapper.toResponse(detalleService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        detalleService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

