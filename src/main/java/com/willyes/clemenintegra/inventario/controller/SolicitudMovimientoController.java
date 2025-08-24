package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoListadoDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import com.willyes.clemenintegra.shared.util.DateParser;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventarios/solicitudes")
@RequiredArgsConstructor
public class SolicitudMovimientoController {

    private final SolicitudMovimientoService service;
    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<SolicitudMovimientoResponseDTO> crear(@RequestBody SolicitudMovimientoRequestDTO dto) {
        return new ResponseEntity<>(service.registrarSolicitud(dto), HttpStatus.CREATED);
    }

    @GetMapping
    // endpoint que lista las solicitudes de movimiento con paginación
    public ResponseEntity<Page<SolicitudMovimientoListadoDTO>> listar(
            @PageableDefault(size = 10, sort = "fechaSolicitud", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) EstadoSolicitudMovimiento estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Long almacenOrigenId,
            @RequestParam(required = false) Long almacenDestinoId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta
    ) {
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaSolicitud", "estado", "id"), "fechaSolicitud");
        LocalDateTime inicio = null;
        LocalDateTime fin = null;
        if ((fechaDesde != null && fechaHasta == null) || (fechaDesde == null && fechaHasta != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren fechaDesde y fechaHasta");
        }
        try {
            if (fechaDesde != null) {
                inicio = DateParser.parseStart(fechaDesde);
                fin = DateParser.parseEnd(fechaHasta);
                if (inicio.isAfter(fin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaDesde no puede ser mayor a fechaHasta");
                }
            }
            Page<SolicitudMovimientoListadoDTO> page = service.listarSolicitudes(estado, busqueda, almacenOrigenId, almacenDestinoId, inicio, fin, sanitized);
            return ResponseEntity.ok(page);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudMovimientoResponseDTO> aprobar(@PathVariable Long id,
                                                                  Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarPorNombreUsuario(username);
        Long usuarioId = usuario.getId();
        return ResponseEntity.ok(service.aprobarSolicitud(id, usuarioId));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudMovimientoResponseDTO> rechazar(@PathVariable Long id,
                                                                   @RequestParam Long responsableId,
                                                                   @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(service.rechazarSolicitud(id, responsableId, observaciones));
    }

    @PutMapping("/{id}/revertir-autorizacion")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudMovimientoResponseDTO> revertir(@PathVariable Long id,
                                                                   Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarPorNombreUsuario(username);
        Long usuarioId = usuario.getId();
        return ResponseEntity.ok(service.revertirAutorizacion(id, usuarioId));
    }
}
