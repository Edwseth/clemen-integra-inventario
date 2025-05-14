package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/ordenes")
public class OrdenProduccionController {

    @Autowired
    private OrdenProduccionService service;

    @GetMapping
    public List<OrdenProduccionResponse> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrdenProduccionResponse> crear(@RequestBody OrdenProduccionRequest request) {
        Producto producto = new Producto(); producto.setId(request.productoId);
        Usuario responsable = new Usuario(); responsable.setId(request.responsableId);
        OrdenProduccion entidad = ProduccionMapper.toEntity(request, producto, responsable);
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.guardar(entidad)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenProduccionResponse> actualizar(@PathVariable Long id, @RequestBody OrdenProduccionRequest request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.productoId);
                    Usuario responsable = new Usuario(); responsable.setId(request.responsableId);
                    OrdenProduccion entidad = ProduccionMapper.toEntity(request, producto, responsable);
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
