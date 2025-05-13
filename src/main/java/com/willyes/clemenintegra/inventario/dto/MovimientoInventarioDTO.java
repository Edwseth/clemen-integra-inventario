package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimientoDetalle;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MovimientoInventarioDTO(

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipoMovimiento,

        String docReferencia,

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El lote es obligatorio")
        Long loteId,

        @NotNull(message = "El almacén es obligatorio")
        Long almacenId,

        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @NotNull(message = "La orden de compra es obligatoria")
        Long ordenCompraId,

        @NotNull(message = "El motivo del movimiento es obligatorio")
        Long motivoMovimientoId,

        @NotNull(message = "El detalle del movimiento es obligatorio")
        TipoMovimientoDetalle tipoMovimientoDetalle,

        @NotNull(message = "El usuario registrador es obligatorio")
        Long registradoPorId

) {}

