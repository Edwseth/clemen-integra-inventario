package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventarios/solicitudes")
@RequiredArgsConstructor
public class SolicitudMovimientoController {

    private final SolicitudMovimientoService service;

    @PostMapping
    public ResponseEntity<SolicitudMovimientoResponseDTO> crear(@RequestBody SolicitudMovimientoRequestDTO dto) {
        return new ResponseEntity<>(service.registrarSolicitud(dto), HttpStatus.CREATED);
    }

    @GetMapping
    // CODEx: endpoint que lista las solicitudes de movimiento
    public ResponseEntity<List<SolicitudMovimientoResponseDTO>> listar(
            @RequestParam(required = false) EstadoSolicitudMovimiento estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        LocalDateTime d = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime h = hasta != null ? hasta.atTime(23,59,59) : null;
        return ResponseEntity.ok(service.listarSolicitudes(estado, d, h));
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudMovimientoResponseDTO> aprobar(@PathVariable Long id,
                                                                  @RequestParam Long responsableId) {
        return ResponseEntity.ok(service.aprobarSolicitud(id, responsableId));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudMovimientoResponseDTO> rechazar(@PathVariable Long id,
                                                                   @RequestParam Long responsableId,
                                                                   @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(service.rechazarSolicitud(id, responsableId, observaciones));
    }
}
