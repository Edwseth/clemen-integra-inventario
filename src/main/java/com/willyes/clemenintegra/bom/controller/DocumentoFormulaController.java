package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/bom/documentos")
@RequiredArgsConstructor
public class DocumentoFormulaController {

    private final DocumentoFormulaApplicationService documentoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public List<DocumentoFormulaResponseDTO> listarTodas() {
        return documentoService.listarTodas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DocumentoFormulaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }


    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DocumentoFormulaResponseDTO> crear(@RequestBody DocumentoFormulaRequestDTO request) {
        return ResponseEntity.ok(documentoService.guardar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DocumentoFormulaResponseDTO> actualizar(@PathVariable Long id, @RequestBody DocumentoFormulaRequestDTO request) {
        DocumentoFormulaResponseDTO actualizado = documentoService.actualizar(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

