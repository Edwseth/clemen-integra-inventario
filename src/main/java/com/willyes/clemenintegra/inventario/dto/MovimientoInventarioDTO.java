package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoInventarioDTO(
        Long id,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad,

        //@NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipoMovimiento,

        String docReferencia,

        @NotNull(message = "El producto es obligatorio")
        Integer productoId,

        //@NotNull(message = "El lote es obligatorio")
        Long loteProductoId,

        //@NotNull(message = "El almacén es obligatorio")
        Integer almacenOrigenId,

        Integer almacenDestinoId,

        //@NotNull(message = "El proveedor es obligatorio")
        Integer proveedorId,

        //@NotNull(message = "La orden de compra es obligatoria")
        Integer ordenCompraId,

        //@NotNull(message = "El motivo del movimiento es obligatorio")
        Long motivoMovimientoId,

        //@NotNull(message = "El detalle del movimiento es obligatorio")
        Long tipoMovimientoDetalleId, // ✅ CORREGIDO

        //@NotNull(message = "El usuario es obligatorio")
        Long usuarioId,

        //@NotNull(message = "El detalle de la orden de compra es obligatorio")
        Long ordenCompraDetalleId,

        String codigoLote,
        LocalDate fechaVencimiento,
        /**
         * Estado inicial sugerido por el cliente. Este valor se ignora en el
         * proceso de registro del movimiento.
         */
        EstadoLote estadoLote
) { }


