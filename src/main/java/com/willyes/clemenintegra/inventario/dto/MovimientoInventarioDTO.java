package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.willyes.clemenintegra.inventario.model.enums.CausaDevolucionPT;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.CondicionProductoDevuelto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.shared.json.LenientLocalDateTimeDeserializer;
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
        String clienteNombre,
        CausaDevolucionPT causaDevolucionPt,
        CondicionProductoDevuelto condicionProductoDevuelto,

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
        Long ordenProduccionEtapaId,
        Long ordenCompraDetalleId,
        String codigoLote,
        /**
         * Fecha asociada a la creación o actualización de lotes (recepciones/ajustes).
         * Para salidas de producto terminado y otros movimientos este campo es opcional
         * y normalmente se omite del payload enviado por el frontend.
         */
        @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
        LocalDateTime fechaVencimiento,
        /**
         * Estado inicial sugerido por el cliente. Este valor se ignora en el
         * proceso de registro del movimiento.
         */
        EstadoLote estadoLote,

        Boolean autoSplit,

        @JsonProperty("atenciones")
        List<AtencionDTO> atenciones,

        Boolean loteLegacy,

        Long ubicacionDestinoId


) {
    public MovimientoInventarioDTO(
            Long id,
            BigDecimal cantidad,
            TipoMovimiento tipoMovimiento,
            ClasificacionMovimientoInventario clasificacionMovimientoInventario,
            String docReferencia,
            String destinoTexto,
            Integer productoId,
            Long loteProductoId,
            Integer almacenOrigenId,
            Integer almacenDestinoId,
            Integer proveedorId,
            Integer ordenCompraId,
            Long motivoMovimientoId,
            Long tipoMovimientoDetalleId,
            Long solicitudMovimientoId,
            Long usuarioId,
            Long ordenProduccionId,
            Long ordenProduccionEtapaId,
            Long ordenCompraDetalleId,
            String codigoLote,
            LocalDateTime fechaVencimiento,
            EstadoLote estadoLote,
            Boolean autoSplit,
            List<AtencionDTO> atenciones,
            Long ubicacionDestinoId
    ) {
        this(
                id,
                cantidad,
                tipoMovimiento,
                clasificacionMovimientoInventario,
                docReferencia,
                destinoTexto,
                null,
                null,
                null,
                productoId,
                loteProductoId,
                almacenOrigenId,
                almacenDestinoId,
                proveedorId,
                ordenCompraId,
                motivoMovimientoId,
                tipoMovimientoDetalleId,
                solicitudMovimientoId,
                usuarioId,
                ordenProduccionId,
                ordenProduccionEtapaId,
                ordenCompraDetalleId,
                codigoLote,
                fechaVencimiento,
                estadoLote,
                autoSplit,
                atenciones,
                null,
                ubicacionDestinoId
        );
    }
}
