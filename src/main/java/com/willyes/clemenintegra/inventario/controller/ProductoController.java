package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.ProductoService;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final MovimientoInventarioService service;
    private final ProductoRepository productoRepo;
    private final LoteProductoRepository loteRepo;


    @GetMapping("/categoria/{nombre}")
    public ResponseEntity<List<ProductoResponseDTO>> buscarPorCategoria(
            @PathVariable String nombre) {
        List<ProductoResponseDTO> productos = productoService.buscarPorCategoria(nombre);
        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(productos);
        }
        return ResponseEntity.ok(productos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROL_JEFE_ALMACENES')")
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

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        ProductoResponseDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROL_JEFE_ALMACENES')")
    public ResponseEntity<ProductoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequestDTO dto) {
        ProductoResponseDTO actualizado = productoService.actualizarProducto(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROL_JEFE_ALMACENES')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

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
    public ResponseEntity<List<ProductoResponseDTO>> obtenerTodos() {
        List<ProductoResponseDTO> productos = productoService.listarTodos();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/reporte-stock")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA')")
    @Operation(summary = "Exportar reporte de stock actual de productos a Excel")
    @ApiResponse(responseCode = "200", description = "Reporte generado correctamente")
    public void exportarStockActual(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=stock_actual.xlsx");

        Workbook workbook = productoService.generarReporteStockActualExcel();
        try (ServletOutputStream out = response.getOutputStream()) {
            workbook.write(out);
            out.flush();
        }
        workbook.close();
    }

}

