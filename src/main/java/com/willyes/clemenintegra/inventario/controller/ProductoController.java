package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION')")
    public ResponseEntity<Page<ProductoOptionDTO>> buscarProductos(
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProductoOptionDTO> page = productoService.buscarOpciones(q, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/categoria/{nombre}")
    public ResponseEntity<List<ProductoResponseDTO>> buscarPorCategoria(
            @PathVariable String nombre) {
        List<ProductoResponseDTO> productos = productoService.buscarPorCategoria(nombre);
        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(productos);
        }
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/producto-terminado")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public List<ProductoResponseDTO> getProductosTerminados() {
        return productoService.findByCategoriaTipo("PRODUCTO_TERMINADO");
    }

    @GetMapping("/insumos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public List<ProductoResponseDTO> getProductosInsumo() {
        return productoService.findByCategoriaTipoIn(List.of("MATERIA_PRIMA", "MATERIAL_EMPAQUE"));
    }

    @GetMapping("/terminados")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public List<ProductoResponseDTO> getProductosTerminadosPublico() {
        return productoService.findByCategoriaTipo("PRODUCTO_TERMINADO");
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoRequestDTO dto) {
        try {
            ProductoResponseDTO creado = productoService.crearProducto(dto);
            return ResponseEntity.status(201).body(creado);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("SKU duplicado o restricción de integridad violada");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error interno al crear producto", e);
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        ProductoResponseDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<ProductoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequestDTO dto) {
        ProductoResponseDTO actualizado = productoService.actualizarProducto(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    // PROD-INACTIVAR BEGIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            productoService.eliminarProducto(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "PRODUCT_HAS_DEPENDENCIES", "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<ProductoResponseDTO> cambiarEstado(@PathVariable Long id,
                                                             @RequestBody ProductoEstadoRequestDTO body) {
        ProductoResponseDTO dto = productoService.actualizarEstado(id, body.activo());
        return ResponseEntity.ok(dto);
    }
    // PROD-INACTIVAR END

    @PutMapping("/{id}/unidad-medida")
    public ResponseEntity<?> cambiarUnidadMedida(
            @PathVariable Long id,
            @RequestBody @Valid UnidadMedidaRequestDTO dto) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        // Verificar si tiene movimientos asociados
        boolean tieneMovimientos = movimientoInventarioRepository.existsByProductoId(id);
        if (tieneMovimientos) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "No se puede modificar la unidad de medida: existen movimientos asociados"));
        }

        // Buscar la nueva unidad o crearla si no existe
        UnidadMedida unidad = unidadMedidaRepository.findByNombre(dto.getNombre())
                .orElseGet(() -> unidadMedidaRepository.save(new UnidadMedida(null, dto.getNombre(), dto.getSimbolo())));

        producto.setUnidadMedida(unidad);
        productoRepository.save(producto);

        return ResponseEntity.ok(new UnidadMedidaResponseDTO(unidad.getId(), unidad.getNombre(), unidad.getSimbolo()));
    }

    @GetMapping("/con-lotes")
    public ResponseEntity<List<ProductoConEstadoLoteDTO>> productosConLotesPorEstado(
            @RequestParam String estado) {
        try {
            List<ProductoConEstadoLoteDTO> productos = productoService.buscarProductosConLotesPorEstado(estado);
            return ResponseEntity.ok(productos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.emptyList()); // estado inválido
        }
    }

    @GetMapping("/agrupado-por-lotes")
    public ResponseEntity<List<ProductoConLotesDTO>> productosAgrupadosPorLotesEnEstado(
            @RequestParam String estado) {
        try {
            List<ProductoConLotesDTO> productos = productoService.buscarProductosConLotesAgrupadosPorEstado(estado);
            return ResponseEntity.ok(productos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN', 'ROL_JEFE_CALIDAD')")
    public ResponseEntity<Page<ProductoResponseDTO>> obtenerTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigoSku,
            @RequestParam(required = false) Long categoriaProductoId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaCreacion", "id", "nombre"), "fechaCreacion");
        Page<ProductoResponseDTO> productos = productoService.listarTodos(nombre, codigoSku, categoriaProductoId, activo, sanitized);
        return ResponseEntity.ok(productos);
    }

}

