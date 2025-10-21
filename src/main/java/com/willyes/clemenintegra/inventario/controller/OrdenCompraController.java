package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.mapper.OrdenCompraMapper;
import com.willyes.clemenintegra.inventario.mapper.HistorialEstadoOrdenMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.OrdenCompraPdfService;
import com.willyes.clemenintegra.inventario.service.OrdenCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping({"/api/inventario/ordenes", "/api/ordenes-compra"})
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraDetalleRepository detalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final OrdenCompraService ordenCompraService;
    private final OrdenCompraPdfService ordenCompraPdfService;
    private final OrdenCompraMapper mapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROL_COMPRADOR')")
    @Transactional
    public ResponseEntity<OrdenCompra> crear(@RequestBody OrdenCompraRequestDTO dto) {
        // 1. Validar proveedor
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proveedor no encontrado"));

        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debe registrar al menos un detalle");
        }
        
        // 2. Crear orden y detalles dentro de la misma transacción
        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .estado(EstadoOrdenCompra.CREADA)
                .fechaOrden(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        orden.setCodigoOrden(ordenCompraService.generarCodigoOrdenCompra());

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

        orden.setDetalles(detalles);

        try {
            OrdenCompra guardada = ordenCompraRepository.save(orden);
            return ResponseEntity.status(CREATED).body(guardada);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Error al guardar la orden de compra");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
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

    @GetMapping
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listar(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/estado")
    public ResponseEntity<Page<OrdenCompraResponseDTO>> listarPorEstado(
            @RequestParam EstadoOrdenCompra estado,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrdenCompraResponseDTO> page = ordenCompraService.listarPorEstado(estado, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/detalles")
    public ResponseEntity<OrdenCompraConDetallesResponse> obtenerOrdenConDetalles(@PathVariable Long id) {
        return ordenCompraService.buscarPorIdConDetalles(id)
                .map(mapper::toOrdenCompraConDetallesResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<HistorialEstadoOrdenResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambioEstadoOrdenRequest request) {
        var historial = ordenCompraService.cambiarEstado(
                id,
                request.estado,
                request.usuarioId,
                request.observaciones);
        return ResponseEntity.ok(HistorialEstadoOrdenMapper.toResponse(historial));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> pdf(@PathVariable("id") Long id) {   // <-- Long
        // 1) Cargar OC con detalles
        var ocOpt = ordenCompraService.buscarPorIdConDetalles(id);
        if (ocOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var oc = ocOpt.get(); // <-- desenvolver Optional

        // 2) Generar PDF con la entidad (no Optional)
        byte[] pdf = ordenCompraPdfService.generarPdf(oc);

        // 3) Nombre de archivo seguro
        String nombre = "OC-" + (oc.getCodigoOrden() != null ? oc.getCodigoOrden() : oc.getId()) + ".pdf";

        // 4) Respuesta
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(pdf);
    }

    private String loadTemplate(String classpathLocation) {        // <- sin guion bajo
        try (InputStream is = new ClassPathResource(classpathLocation).getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se encontró la plantilla: " + classpathLocation, e);
        }
    }

    private static java.math.BigDecimal bd(Number n) {
        return n == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(n.toString());
    }
    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

}

