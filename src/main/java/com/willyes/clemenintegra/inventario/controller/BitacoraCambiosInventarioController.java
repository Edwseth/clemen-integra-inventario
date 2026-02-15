package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.service.BitacoraCambiosInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/bitacora")
@RequiredArgsConstructor
// TODO:REMOVE_AFTER_INV_FULL_MIGRATION
@org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('INV_READ','INV_WRITE','INV_DECIDE','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
public class BitacoraCambiosInventarioController {

    private final BitacoraCambiosInventarioService service;

    @GetMapping
    public ResponseEntity<List<BitacoraCambiosInventarioDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<BitacoraCambiosInventarioDTO> crear(@RequestBody BitacoraCambiosInventarioDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
