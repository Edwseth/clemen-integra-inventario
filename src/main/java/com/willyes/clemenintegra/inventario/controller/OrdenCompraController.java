package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraDetalleRepository detalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    @PostMapping
    public ResponseEntity<OrdenCompra> crear(@RequestBody OrdenCompraRequestDTO dto) {
        // 1. Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proveedor no encontrado"));

        // 2. Crear orden sin detalles primero
        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDate.now())
                .observaciones(dto.getObservaciones())
                .build();

        ordenCompraRepository.save(orden); // Necesario para generar el ID

        // 3. Crear y asociar detalles
        List<OrdenCompraDetalle> detalles = dto.getDetalles().stream().map(d -> {
            Producto producto = productoRepository.findById(d.getProductoId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado"));

            BigDecimal valorTotal = d.getValorUnitario().multiply(d.getCantidad());

            return OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();
        }).toList();

        detalleRepository.saveAll(detalles);

        return ResponseEntity.status(CREATED).body(orden);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROL_JEFE_ALMACENES')")
    public ResponseEntity<OrdenCompra> actualizar(@PathVariable Long id,
                                                  @RequestBody OrdenCompraRequestDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));

        // Actualizar proveedor si cambió
        if (!orden.getProveedor().getId().equals(dto.getProveedorId())) {
            Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
            orden.setProveedor(proveedor);
        }

        orden.setObservaciones(dto.getObservaciones());

        // Eliminar detalles anteriores
        detalleRepository.deleteByOrdenCompra_Id(id);

        // Crear y guardar nuevos detalles
        List<OrdenCompraDetalle> nuevosDetalles = dto.getDetalles().stream().map(d -> {
            Producto producto = productoRepository.findById(d.getProductoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            BigDecimal valorTotal = d.getValorUnitario().multiply(d.getCantidad());

            return OrdenCompraDetalle.builder()
                    .ordenCompra(orden)
                    .producto(producto)
                    .cantidad(d.getCantidad())
                    .valorUnitario(d.getValorUnitario())
                    .valorTotal(valorTotal)
                    .iva(d.getIva())
                    .cantidadRecibida(BigDecimal.ZERO)
                    .build();
        }).toList();

        detalleRepository.saveAll(nuevosDetalles);

        ordenCompraRepository.save(orden);
        return ResponseEntity.ok(orden);
    }

}

