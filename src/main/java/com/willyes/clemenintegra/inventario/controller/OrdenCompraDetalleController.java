package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraDetalleMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraRepository;
import com.willyes.clemenintegra.inventario.repository.ProductoRepository;
import com.willyes.clemenintegra.inventario.service.OrdenCompraDetalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventario/ordenes-compra-detalle")
public class OrdenCompraDetalleController {

    @Autowired private OrdenCompraDetalleService service;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private ProductoRepository productoRepository;

    @GetMapping
    public List<OrdenCompraDetalleResponse> listarTodos() {
        return service.listarTodos().stream()
                .map(OrdenCompraDetalleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<OrdenCompraDetalleResponse> crear(@RequestBody OrdenCompraDetalleRequestDTO request) {
        OrdenCompra orden = ordenCompraRepository.findById(request.getOrdenCompraId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de compra no encontrada"));

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        BigDecimal valorTotal = request.getValorUnitario().multiply(request.getCantidad());
        BigDecimal cantidadRecibida = BigDecimal.ZERO;

        OrdenCompraDetalle entidad = OrdenCompraDetalle.builder()
                .cantidad(request.getCantidad())
                .valorUnitario(request.getValorUnitario())
                .valorTotal(valorTotal)
                .iva(request.getIva())
                .cantidadRecibida(cantidadRecibida)
                .ordenCompra(orden)
                .producto(producto)
                .build();

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


