package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION','ROL_COMPRADOR'," +
            "'ROL_JEFE_CALIDAD','ROL_MICROBIOLOGO','ROL_ANALISTA_CALIDAD')")
    public ResponseEntity<Page<ProductoOptionDTO>> buscarProductos(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "term", required = false) String term,
            @RequestParam(name = "activo", required = false) Boolean activo,
            @RequestParam(name = "almacenId", required = false) Long almacenId,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        String criterio = (term != null && !term.isBlank()) ? term : q;
        Page<ProductoOptionDTO> page = productoService.buscarOpciones(criterio, activo, almacenId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/buscar-insumos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<ProductoResumenDTO>> buscarInsumos(
            @RequestParam(name = "term") String term,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        validarTerminoBusqueda(term);
        Page<ProductoResumenDTO> page = productoService.buscarInsumos(term, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/buscar-pt")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<ProductoResumenDTO>> buscarProductosTerminados(
            @RequestParam(name = "term") String term,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        validarTerminoBusqueda(term);
        Page<ProductoResumenDTO> page = productoService.buscarProductosTerminados(term, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/buscar-fabricables")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_ALMACENES','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<ProductoAutocompleteDTO>> buscarProductosFabricablesAutocomplete(
            @RequestParam("term") String term,
            Pageable pageable
    ) {
        Page<ProductoAutocompleteDTO> page = productoService.buscarProductosFabricablesAutocomplete(term, pageable);
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

    @GetMapping("/fabricables")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_ALMACENES','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<ProductoResponseDTO>> getProductosFabricables() {
        List<ProductoResponseDTO> productos = productoService.findProductosFabricables();
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
        return productoService.findByCategoriaTipoIn(List.of(
                "MATERIA_PRIMA",
                "MATERIAL_EMPAQUE",
                "SUMINISTROS",
                "PRODUCTO_SEMI_ELABORADO"
        ));
    }

    @GetMapping("/insumos/autocomplete")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<InsumoAutocompleteDTO>> buscarInsumosAutocomplete(
            @RequestParam("term") String term,
            Pageable pageable
    ) {
        Page<InsumoAutocompleteDTO> page = productoService.buscarInsumosAutocomplete(term, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/terminados")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public List<ProductoResponseDTO> getProductosTerminadosPublico() {
        return productoService.findByCategoriaTipo("PRODUCTO_TERMINADO");
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoRequestDTO dto,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        try {
            ProductoResponseDTO creado = productoService.crearProducto(dto, principal.getId());
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
    public ResponseEntity<ProductoResponseDTO> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody ProductoRequestDTO dto,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        ProductoResponseDTO actualizado = productoService.actualizarProducto(id, dto, principal.getId());
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

        // Buscar la unidad por nombre o crearla si no existe
        UnidadMedida unidad = unidadMedidaRepository.findByNombre(dto.getNombre())
                .orElseGet(() -> {
                    UnidadMedida u = new UnidadMedida();          // ctor válido
                    u.setNombre(dto.getNombre().trim());          // setea campos con setters
                    u.setSimbolo(dto.getSimbolo().trim());
                    // Si tu entidad tiene 'codigo' o 'descripcion', puedes setearlos aquí.
                    return unidadMedidaRepository.save(u);
                });

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
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN', 'ROL_JEFE_CALIDAD', 'ROL_JEFE_PRODUCCION')")
    public ResponseEntity<Page<ProductoResponseDTO>> obtenerTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false, name = "codigoSku") String codigoSkuLegacy,
            @RequestParam(required = false) Long categoriaProductoId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaCreacion", "id", "nombre"), "fechaCreacion");
        String finalSku = sku != null ? sku : codigoSkuLegacy;
        Page<ProductoResponseDTO> productos = productoService.listarTodos(nombre, finalSku, categoriaProductoId, activo, sanitized);
        return ResponseEntity.ok(productos);
    }

    private void validarTerminoBusqueda(String term) {
        if (term == null || term.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar al menos 2 caracteres para la búsqueda");
        }
    }

}
