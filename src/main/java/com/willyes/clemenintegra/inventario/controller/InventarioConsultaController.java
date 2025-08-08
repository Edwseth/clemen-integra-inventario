package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.DisponibilidadProductoResponseDTO;
import com.willyes.clemenintegra.inventario.service.InventarioConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/disponibilidad")
@RequiredArgsConstructor
public class InventarioConsultaController {

    private final InventarioConsultaService inventarioConsultaService;

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<DisponibilidadProductoResponseDTO> obtenerDisponibilidadPorProducto(@PathVariable Long productoId) {
        try {
            DisponibilidadProductoResponseDTO dto = inventarioConsultaService.obtenerDisponibilidadPorProducto(productoId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
