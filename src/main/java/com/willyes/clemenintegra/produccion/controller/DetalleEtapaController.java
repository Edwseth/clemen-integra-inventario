package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.service.*;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/detalles")
public class DetalleEtapaController {

    @Autowired
    private DetalleEtapaService service;

    @GetMapping
    public List<DetalleEtapaResponse> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleEtapaResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DetalleEtapaResponse> crear(@RequestBody DetalleEtapaRequest request) {
        EtapaProduccion etapa = new EtapaProduccion(); etapa.setId(request.etapaProduccionId);
        OrdenProduccion orden = new OrdenProduccion(); orden.setId(request.ordenProduccionId);
        Usuario operario = new Usuario(); operario.setId(request.operarioId);
        DetalleEtapa entidad = ProduccionMapper.toEntity(request, etapa, orden, operario);
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleEtapaResponse> actualizar(@PathVariable Long id, @RequestBody DetalleEtapaRequest request) {
        return service.buscarPorId(id)
                .map(existente -> {
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
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
