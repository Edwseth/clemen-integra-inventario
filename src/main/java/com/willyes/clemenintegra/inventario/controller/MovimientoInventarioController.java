package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;
    private final ProductoRepository productoRepo;
    private final LoteProductoRepository loteRepo;

    @Operation(summary = "Registrar un movimiento de inventario")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado correctamente")
    @ApiResponse(responseCode = "409", description = "No hay suficiente stock disponible")
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody @Valid MovimientoInventarioDTO dto) {
        // 1) Validación de stock aquí, antes de llamar al servicio:
        var tipo = dto.tipoMovimiento();
        boolean isSalida = tipo.name().startsWith("SALIDA")
                || tipo == ClasificacionMovimientoInventario.AJUSTE_NEGATIVO;

        if (isSalida) {
            Producto prod = productoRepo.findById(dto.productoId())
                    .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
            LoteProducto lote = loteRepo.findById(dto.loteProductoId())
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

            BigDecimal cant = dto.cantidad();
            BigDecimal stockProd = prod.getStockActual();
            BigDecimal stockLote = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);

            if (stockProd.compareTo(cant) < 0 || stockLote.compareTo(cant) < 0) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "No hay suficiente stock disponible"));
            }
        }

        // 2) Si pasa validación, delegamos al servicio para grabar
        MovimientoInventarioDTO creado = service.registrarMovimiento(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }


    @Operation(summary = "Consultar movimientos de inventario con filtros opcionales")
    @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    @GetMapping("/filtrar")
    public ResponseEntity<Page<MovimientoInventario>> filtrar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) TipoMovimiento tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @PageableDefault(size = 10, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        MovimientoInventarioFiltroDTO filtro = new MovimientoInventarioFiltroDTO(
                productoId, almacenId, tipoMovimiento, fechaInicio, fechaFin
        );
        Page<MovimientoInventario> resultados = service.consultarMovimientosConFiltros(filtro, pageable);
        return ResponseEntity.ok(resultados);
    }
}

