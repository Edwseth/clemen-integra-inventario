package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.ProduccionSimpleMapper;
import com.willyes.clemenintegra.produccion.model.Produccion;
import com.willyes.clemenintegra.produccion.service.ProduccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produccion/registro")
public class ProduccionController {

    @Autowired private ProduccionService service;

    @GetMapping
    public List<ProduccionResponse> listarTodas() {
        return service.listarTodas().stream()
                .map(ProduccionSimpleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<ProduccionResponse> crear(@RequestBody ProduccionRequest request) {
        Usuario usuario = new Usuario(); usuario.setId(request.usuarioId);
        Producto producto = new Producto(); producto.setId(request.productoId);
        Produccion entidad = ProduccionSimpleMapper.toEntity(request, usuario, producto);
        return ResponseEntity.ok(ProduccionSimpleMapper.toResponse(service.guardar(entidad)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduccionResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionSimpleMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

