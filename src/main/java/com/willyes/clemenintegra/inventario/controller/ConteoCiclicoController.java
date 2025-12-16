package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoDetalleRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoLoteResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.ConteoCiclicoUpdateRequestDTO;
import com.willyes.clemenintegra.inventario.service.ConteoCiclicoService;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/conteos")
@RequiredArgsConstructor
public class ConteoCiclicoController {

    private final ConteoCiclicoService conteoCiclicoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<Page<ConteoCiclicoResponseDTO>> listar(
            @RequestParam(required = false) Integer almacenId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaCreacion", "id"), "fechaCreacion");
        Page<ConteoCiclicoResponseDTO> respuesta = conteoCiclicoService.listar(almacenId, estado, sanitized);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> obtenerPorId(@PathVariable Long id) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.obtenerPorId(id);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}/lotes")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<List<ConteoCiclicoLoteResponseDTO>> listarLotes(
            @PathVariable Long id,
            @RequestParam Long productoId,
            @RequestParam(required = false) Long ubicacionFisicaId,
            @RequestParam(required = false, name = "q") String texto) {
        List<ConteoCiclicoLoteResponseDTO> respuesta = conteoCiclicoService
                .listarLotesParaConteo(id, productoId, ubicacionFisicaId, texto);
        return ResponseEntity.ok(respuesta);
    }

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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody ConteoCiclicoUpdateRequestDTO request) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.actualizarConteo(id, request != null ? request.getDetalles() : null);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_CONTADOR')")
    public ResponseEntity<ConteoCiclicoResponseDTO> iniciar(@PathVariable Long id) {
        ConteoCiclicoResponseDTO respuesta = conteoCiclicoService.marcarEnConteo(id);
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
