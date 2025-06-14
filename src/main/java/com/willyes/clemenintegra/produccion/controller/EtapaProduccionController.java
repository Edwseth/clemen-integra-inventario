package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/etapas")
public class EtapaProduccionController {

    @Autowired
    private EtapaProduccionService service;
    @Autowired
    private OrdenProduccionService ordenService;

    @GetMapping
    public List<EtapaProduccionResponse> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtapaProduccionResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EtapaProduccionResponse> crear(@RequestBody EtapaProduccionRequest request) {
        OrdenProduccion orden = new OrdenProduccion(); orden.setId(request.ordenProduccionId);
        EtapaProduccion entidad = ProduccionMapper.toEntity(request, orden);
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtapaProduccionResponse> actualizar(@PathVariable Long id, @RequestBody EtapaProduccionRequest request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    OrdenProduccion orden = new OrdenProduccion(); orden.setId(request.ordenProduccionId);
                    EtapaProduccion entidad = ProduccionMapper.toEntity(request, orden);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}