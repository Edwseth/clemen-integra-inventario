package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.service.*;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/detalles")
@RequiredArgsConstructor
public class DetalleEtapaController {

    private final DetalleEtapaService service;
    private final com.willyes.clemenintegra.shared.service.UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public List<DetalleEtapaResponse> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleEtapaResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleEtapaResponse> crear(@Valid @RequestBody DetalleEtapaRequest request) {
        normalizarRequest(request, null);
        EtapaProduccion etapa = new EtapaProduccion(); etapa.setId(request.etapaProduccionId);
        OrdenProduccion orden = new OrdenProduccion(); orden.setId(request.ordenProduccionId);
        Usuario operario = new Usuario(); operario.setId(request.operarioId);
        DetalleEtapa entidad = ProduccionMapper.toEntity(request, etapa, orden, operario);
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<DetalleEtapaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody DetalleEtapaRequest request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    normalizarRequest(request, existente);
                    EtapaProduccion etapa = new EtapaProduccion(); etapa.setId(request.etapaProduccionId);
                    OrdenProduccion orden = new OrdenProduccion(); orden.setId(request.ordenProduccionId);
                    Usuario operario = new Usuario(); operario.setId(request.operarioId);
                    DetalleEtapa entidad = ProduccionMapper.toEntity(request, etapa, orden, operario);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void normalizarRequest(DetalleEtapaRequest request, DetalleEtapa existente) {
        if (request == null) {
            return;
        }
        // La fecha de inicio y el operario se fijan en backend para evitar discrepancias
        // de zona horaria y asegurar trazabilidad de quién inició la etapa.
        request.fechaInicio = existente != null ? existente.getFechaInicio() : LocalDateTime.now();
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        request.operarioId = usuario != null ? usuario.getId() : null;
        if (StringUtils.hasText(request.operarioNombre)) {
            String prefijo = "Operario: " + request.operarioNombre;
            request.observaciones = StringUtils.hasText(request.observaciones)
                    ? request.observaciones + " | " + prefijo
                    : prefijo;
        }
    }
}
