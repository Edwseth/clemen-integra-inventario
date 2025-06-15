package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/ordenes")
@RequiredArgsConstructor
public class OrdenProduccionController {

    private final OrdenProduccionService service;

    @GetMapping
    public List<OrdenProduccionResponseDTO> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrdenProduccionResponseDTO> crear(@RequestBody OrdenProduccionRequestDTO request) {
        Producto producto = new Producto(); producto.setId(request.getProductoId());
        Usuario responsable = new Usuario(); responsable.setId(request.getResponsableId());
        OrdenProduccion entidad = ProduccionMapper.toEntity(request, producto, responsable);
        return new ResponseEntity<>(ProduccionMapper.toResponse(service.guardarConValidacionStock(entidad)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponseDTO> actualizar(@PathVariable Long id, @RequestBody OrdenProduccionRequestDTO request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.getProductoId());
                    Usuario responsable = new Usuario(); responsable.setId(request.getResponsableId());
                    OrdenProduccion entidad = ProduccionMapper.toEntity(request, producto, responsable);
                    entidad.setId(existente.getId());
                    return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardarConValidacionStock(entidad)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
