package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraDetalleMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.service.OrdenCompraDetalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventario/ordenes-compra-detalle")
public class OrdenCompraDetalleController {

    @Autowired private OrdenCompraDetalleService service;

    @GetMapping
    public List<OrdenCompraDetalleResponse> listarTodos() {
        return service.listarTodos().stream()
                .map(OrdenCompraDetalleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<OrdenCompraDetalleResponse> crear(@RequestBody OrdenCompraDetalleRequest request) {
        OrdenCompra orden = new OrdenCompra(); orden.setId(request.ordenCompraId);
        Producto producto = new Producto(); producto.setId(request.productoId);
        OrdenCompraDetalle entidad = OrdenCompraDetalleMapper.toEntity(request, orden, producto);
        return ResponseEntity.ok(OrdenCompraDetalleMapper.toResponse(service.guardar(entidad)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraDetalleResponse> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(OrdenCompraDetalleMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

