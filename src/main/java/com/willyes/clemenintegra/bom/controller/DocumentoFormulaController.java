package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.mapper.BomMapper;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bom/documentos")
@RequiredArgsConstructor
public class DocumentoFormulaController {

    private final DocumentoFormulaService documentoService;

    @GetMapping
    public List<DocumentoFormulaResponse> listarTodas() {
        return documentoService.listarTodas().stream()
                .map(BomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoFormulaResponse> obtenerPorId(@PathVariable Long id) {
        return documentoService.buscarPorId(id)
                .map(BomMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DocumentoFormulaResponse> crear(@RequestBody DocumentoFormulaRequest request) {
        FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
        DocumentoFormula entidad = BomMapper.toEntity(request, formula);
        return ResponseEntity.ok(BomMapper.toResponse(documentoService.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoFormulaResponse> actualizar(@PathVariable Long id, @RequestBody DocumentoFormulaRequest request) {
        return documentoService.buscarPorId(id)
                .map(existente -> {
                    FormulaProducto formula = new FormulaProducto(); formula.setId(request.formulaId);
                    DocumentoFormula entidad = BomMapper.toEntity(request, formula);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(BomMapper.toResponse(documentoService.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

