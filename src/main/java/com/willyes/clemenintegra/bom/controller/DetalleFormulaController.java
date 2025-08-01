package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/detalles")
@RequiredArgsConstructor
public class DetalleFormulaController {

    private final DetalleFormulaService detalleService;
    private final BomMapper bomMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public List<DetalleFormulaResponse> listarTodas() {
        return detalleService.listarTodas().stream()
                .map(detalle -> bomMapper.toResponse(detalle))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleFormulaResponse> obtenerPorId(@PathVariable Long id) {
        return detalleService.buscarPorId(id)
                .map(detalle -> bomMapper.toResponse(detalle))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleFormulaResponse> crear(@RequestBody DetalleFormulaRequest request) {
        FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
        Producto insumo = new Producto(); insumo.setId(request.insumoId.intValue());
        UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
        DetalleFormula entidad = bomMapper.toEntity(request, formula, insumo, unidad);
        return ResponseEntity.ok(bomMapper.toResponse(detalleService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleFormulaResponse> actualizar(@PathVariable Long id, @RequestBody DetalleFormulaRequest request) {
        return detalleService.buscarPorId(id)
                .map(existente -> {
                    FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
                    Producto insumo = new Producto(); insumo.setId(request.insumoId.intValue());
                    UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
                    DetalleFormula entidad = bomMapper.toEntity(request, formula, insumo, unidad);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(bomMapper.toResponse(detalleService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        detalleService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

