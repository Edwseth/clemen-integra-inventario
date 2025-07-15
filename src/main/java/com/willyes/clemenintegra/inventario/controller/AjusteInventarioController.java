package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.service.AjusteInventarioService;
import com.willyes.clemenintegra.shared.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/ajustes")
@RequiredArgsConstructor
public class AjusteInventarioController {

    private final AjusteInventarioService service;

    @GetMapping
    public ResponseEntity<List<AjusteInventarioResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<AjusteInventarioResponseDTO> crear(@RequestBody @Valid AjusteInventarioRequestDTO dto,
                                                             @AuthenticationPrincipal(expression = "usuario") Usuario usuario) {
        return ResponseEntity.ok(service.crear(dto, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

