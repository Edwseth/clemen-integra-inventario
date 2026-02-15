package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoCalidadUpdateDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
public class ProductoCalidadController {

    private final ProductoService productoService;

    @PatchMapping("/{id}/calidad")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<ProductoResponseDTO> actualizarCamposCalidad(
            @PathVariable Long id,
            @Valid @RequestBody ProductoCalidadUpdateDTO dto) {
        ProductoResponseDTO actualizado = productoService.actualizarCamposCalidad(id, dto);
        return ResponseEntity.ok(actualizado);
    }
}
