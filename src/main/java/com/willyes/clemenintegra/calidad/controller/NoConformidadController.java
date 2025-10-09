package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calidad/no-conformidades")
@RequiredArgsConstructor
public class NoConformidadController {

    private final NoConformidadService service;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<Page<NoConformidadDTO>> listar(
            @RequestParam(required = false) SeveridadNoConformidad severidad,
            @RequestParam(required = false) OrigenNoConformidad origen,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(severidad, origen, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoConformidadDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<NoConformidadDTO> crear(
            @RequestBody NoConformidadDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Usuario authUser = null;
        if (userDetails != null) {
            dto.setUsuarioReportaId(userDetails.getId());
            authUser = usuarioRepository.findById(userDetails.getId()).orElse(null);
        }
        return ResponseEntity.ok(service.crear(dto, authUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<NoConformidadDTO> actualizar(
            @PathVariable Long id,
            @RequestBody NoConformidadDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            dto.setUsuarioReportaId(userDetails.getId());
        }
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

