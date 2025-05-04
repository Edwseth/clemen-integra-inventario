package com.willyes.clemenintegra.inventario.api.controller;

import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.application.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.domain.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.domain.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    @Operation(summary = "Registrar un movimiento de inventario")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado correctamente")
    @PostMapping
    public ResponseEntity<MovimientoInventario> registrar(@RequestBody @Valid MovimientoInventarioDTO dto) {
        MovimientoInventario creado = service.registrarMovimiento(dto);
        return ResponseEntity.status(201).body(creado);
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

