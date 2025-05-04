package com.willyes.clemenintegra.inventario.application.dto;

import com.willyes.clemenintegra.inventario.domain.enums.TipoMovimiento;
import java.time.LocalDate;

public record MovimientoInventarioFiltroDTO(
        Long productoId,
        Long almacenId,
        TipoMovimiento tipoMovimiento,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {}

