package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/bom/documentos")
@RequiredArgsConstructor
public class DocumentoFormulaController {

    private final DocumentoFormulaApplicationService documentoService;

    @GetMapping
    public List<DocumentoFormulaResponseDTO> listarTodas() {
        return documentoService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoFormulaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }


    @PostMapping
    public ResponseEntity<DocumentoFormulaResponseDTO> crear(@RequestBody DocumentoFormulaRequestDTO request) {
        return ResponseEntity.ok(documentoService.guardar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoFormulaResponseDTO> actualizar(@PathVariable Long id, @RequestBody DocumentoFormulaRequestDTO request) {
        DocumentoFormulaResponseDTO actualizado = documentoService.actualizar(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

