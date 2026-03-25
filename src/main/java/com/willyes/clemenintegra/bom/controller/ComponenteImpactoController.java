package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.ComponenteImpactoResponseDTO;
import com.willyes.clemenintegra.bom.service.ComponenteImpactoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bom/componentes")
@RequiredArgsConstructor
public class ComponenteImpactoController {

    private final ComponenteImpactoService componenteImpactoService;

    @GetMapping("/{productoId}/impacto")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ')")
    public ResponseEntity<ComponenteImpactoResponseDTO> obtenerImpacto(@PathVariable Long productoId) {
        return ResponseEntity.ok(componenteImpactoService.obtenerImpactoPorProductoId(productoId));
    }
}
