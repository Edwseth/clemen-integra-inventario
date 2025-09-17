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
    public static class AtencionDTO {
        private Long detalleId;
        private Long loteId;
        private BigDecimal cantidad;
        private Integer almacenOrigenId;
        private Integer almacenDestinoId;

        public Long getDetalleId() {
            return detalleId;
        }

        public void setDetalleId(Long detalleId) {
            this.detalleId = detalleId;
        }

        public Long getLoteId() {
            return loteId;
        }

        public void setLoteId(Long loteId) {
            this.loteId = loteId;
        }

        public BigDecimal getCantidad() {
            return cantidad;
        }

        public void setCantidad(BigDecimal cantidad) {
            this.cantidad = cantidad;
        }

        public Integer getAlmacenOrigenId() {
            return almacenOrigenId;
        }

        public void setAlmacenOrigenId(Integer almacenOrigenId) {
            this.almacenOrigenId = almacenOrigenId;
        }

        public Integer getAlmacenDestinoId() {
            return almacenDestinoId;
        }

        public void setAlmacenDestinoId(Integer almacenDestinoId) {
            this.almacenDestinoId = almacenDestinoId;
        }
    }
}


