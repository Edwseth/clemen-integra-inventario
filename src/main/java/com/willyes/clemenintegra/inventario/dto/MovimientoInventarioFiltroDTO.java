package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import java.time.LocalDate;

public record MovimientoInventarioFiltroDTO(
        Long productoId,
        Long almacenId,
        TipoMovimiento tipoMovimiento,
        ClasificacionMovimientoInventario clasificacion,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {}

