package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;
import com.willyes.clemenintegra.inventario.service.TipoMovimientoDetalleService;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/tipos-movimiento-detalle")
@RequiredArgsConstructor
public class TipoMovimientoDetalleController {

    private final TipoMovimientoDetalleService service;

    @GetMapping
    public ResponseEntity<Page<TipoMovimientoDetalleDTO>> listar(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("id"), "id");
        return ResponseEntity.ok(service.listarTodos(sanitized));
    }

    @PostMapping
    public ResponseEntity<TipoMovimientoDetalleDTO> crear(@RequestBody TipoMovimientoDetalleDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
