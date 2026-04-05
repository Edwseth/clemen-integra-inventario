package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import com.willyes.clemenintegra.inventario.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bom/detalles")
@RequiredArgsConstructor
public class DetalleFormulaController {
    // TODO(rbac-bom-cut1): retirar fallback por roles y permisos granulares BOM_* legacy al finalizar migracion canonica.

    private final DetalleFormulaService detalleService;
    private final BomMapper bomMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ')")
    public Page<DetalleFormulaResponse> listarTodas(
            @RequestParam(required = false) Long formulaId,
            @RequestParam(required = false) String formulaNombre,
            @RequestParam(required = false) String insumo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return detalleService.listarTodas(formulaId, formulaNombre, insumo, pageable)
                .map(bomMapper::toResponseDTO);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ')")
    public ResponseEntity<DetalleFormulaResponse> obtenerPorId(@PathVariable Long id) {
        return detalleService.buscarPorId(id)
                .map(detalle -> bomMapper.toResponseDTO(detalle))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE')")
    public ResponseEntity<DetalleFormulaResponse> crear(@RequestBody DetalleFormulaRequest request) {
        FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
        Producto insumo = new Producto(); insumo.setId(request.insumoId.intValue());
        UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
        DetalleFormula entidad = bomMapper.toEntity(request, formula, insumo, unidad);
        return ResponseEntity.ok(bomMapper.toResponseDTO(detalleService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE')")
    public ResponseEntity<DetalleFormulaResponse> actualizar(@PathVariable Long id, @RequestBody DetalleFormulaRequest request) {
        return detalleService.buscarPorId(id)
                .map(existente -> {
                    FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
                    Producto insumo = new Producto(); insumo.setId(request.insumoId.intValue());
                    UnidadMedida unidad = new UnidadMedida(); unidad.setId(request.unidadMedidaId);
                    DetalleFormula entidad = bomMapper.toEntity(request, formula, insumo, unidad);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(bomMapper.toResponseDTO(detalleService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        detalleService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
