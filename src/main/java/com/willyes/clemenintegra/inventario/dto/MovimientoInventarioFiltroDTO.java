package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import java.time.LocalDateTime;

public record MovimientoInventarioFiltroDTO(
        Long productoId,
        Long almacenId,
        TipoMovimiento tipoMovimiento,
        ClasificacionMovimientoInventario clasificacion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin
) {}

