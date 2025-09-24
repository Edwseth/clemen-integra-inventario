package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MovimientoInventarioDTO(
        Long id,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad,
        TipoMovimiento tipoMovimiento,
        ClasificacionMovimientoInventario clasificacionMovimientoInventario,
        String docReferencia,
        String destinoTexto,

        @NotNull(message = "El producto es obligatorio")
        Integer productoId,
        Long loteProductoId,
        Integer almacenOrigenId,
        Integer almacenDestinoId,
        Integer proveedorId,
        Integer ordenCompraId,
        Long motivoMovimientoId,
        Long tipoMovimientoDetalleId,
        @JsonProperty("solicitudMovimientoId")
        Long solicitudMovimientoId,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Long usuarioId,
        Long ordenProduccionId,
        Long ordenCompraDetalleId,
        String codigoLote,
        LocalDateTime fechaVencimiento,
        /**
         * Estado inicial sugerido por el cliente. Este valor se ignora en el
         * proceso de registro del movimiento.
         */
        EstadoLote estadoLote,

        Boolean autoSplit,

        @JsonProperty("atenciones")
        List<AtencionDTO> atenciones


) {
}


