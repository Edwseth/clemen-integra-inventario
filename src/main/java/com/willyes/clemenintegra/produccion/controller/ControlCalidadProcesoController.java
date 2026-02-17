package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.ControlCalidadProcesoRequest;
import com.willyes.clemenintegra.produccion.dto.ControlCalidadProcesoResponse;
import com.willyes.clemenintegra.produccion.mapper.ControlCalidadProcesoMapper;
import com.willyes.clemenintegra.produccion.model.ControlCalidadProceso;
import com.willyes.clemenintegra.produccion.model.DetalleEtapa;
import com.willyes.clemenintegra.produccion.service.ControlCalidadProcesoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/calidad")
@RequiredArgsConstructor
public class ControlCalidadProcesoController {

    private final ControlCalidadProcesoService service;

    @GetMapping
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_READ')")
    public List<ControlCalidadProcesoResponse> listarTodas() {
        return service.listarTodas()
                .stream()
                .map(ControlCalidadProcesoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_READ')")
    public ResponseEntity<ControlCalidadProcesoResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ControlCalidadProcesoMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_READ')")
    public ResponseEntity<ControlCalidadProcesoResponse> crear(@RequestBody ControlCalidadProcesoRequest request) {
        DetalleEtapa detalle = new DetalleEtapa();
        detalle.setId(request.detalleEtapaId);
        Usuario evaluador = new Usuario();
        evaluador.setId(request.evaluadorId);

        ControlCalidadProceso entidad = ControlCalidadProcesoMapper.toEntity(request, detalle, evaluador);
        return ResponseEntity.ok(ControlCalidadProcesoMapper.toResponse(service.guardar(entidad)));
    }

    @PutMapping("/{id}")
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<ControlCalidadProcesoResponse> actualizar(@PathVariable Long id, @RequestBody ControlCalidadProcesoRequest request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    DetalleEtapa detalle = new DetalleEtapa();
                    detalle.setId(request.detalleEtapaId);
                    Usuario evaluador = new Usuario();
                    evaluador.setId(request.evaluadorId);

                    ControlCalidadProceso entidad = ControlCalidadProcesoMapper.toEntity(request, detalle, evaluador);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(ControlCalidadProcesoMapper.toResponse(service.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_WRITE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

