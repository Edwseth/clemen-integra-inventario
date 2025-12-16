package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/conteos")
@RequiredArgsConstructor
public class ConteoCiclicoController {

    private final ConteoCiclicoService conteoCiclicoService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> crear(@Valid @RequestBody ConteoCiclicoRequestDTO request) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.crearConteo(request);
        return ResponseEntity.status(201).body(respuesta);
    }

    @PostMapping("/{id}/detalles")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> agregarDetalles(@PathVariable Long id,
                                                                    @Valid @RequestBody List<ConteoCiclicoDetalleRequestDTO> detalles) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.agregarDetalles(id, detalles);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/en-conteo")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> marcarEnConteo(@PathVariable Long id) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.marcarEnConteo(id);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> cerrar(@PathVariable Long id) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.cerrar(id);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/aplicar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> aplicar(@PathVariable Long id,
                                                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.aplicar(id, idempotencyKey);
        return ResponseEntity.ok(respuesta);
    }
}
