package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.service.AjusteInventarioService;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/ajustes")
@RequiredArgsConstructor
public class AjusteInventarioController {

    private final AjusteInventarioService service;

    @GetMapping
    public ResponseEntity<Page<AjusteInventarioResponseDTO>> listar(
            @PageableDefault(size = 10, sort = "fecha") Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fecha"), "fecha");
        return ResponseEntity.ok(service.listar(sanitized));
    }

    @PostMapping
    public ResponseEntity<AjusteInventarioResponseDTO> crear(@RequestBody @Valid AjusteInventarioRequestDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

