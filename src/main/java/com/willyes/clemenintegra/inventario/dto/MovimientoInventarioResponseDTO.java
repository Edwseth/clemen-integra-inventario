package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioResponseDTO {
    private Long id;
    private LocalDateTime fechaIngreso;
    private TipoMovimiento tipoMovimiento;
    private String clasificacion;
    private BigDecimal cantidad;
    private Long productoId;
    private String nombreProducto;
    @JsonProperty("sku")
    @JsonAlias("codigoSku")
    private String sku;
    private Long loteId;
    private String codigoLote;
    private String nombreAlmacenOrigen;
    private String nombreAlmacenDestino;
    private String nombreUsuarioRegistrador;
    private String unidad;
    private Long ordenProduccionId;
    private Long solicitudId;
    private EstadoSolicitudMovimiento estadoSolicitud;
    private List<SolicitudDetalleAtencionDTO> detallesSolicitud;


    @JsonProperty("codigoSku")
    public String getCodigoSku() {return sku;}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SolicitudDetalleAtencionDTO {
        private Long detalleId;
        private Long loteId;
        private String codigoLote;
        private boolean atendida;
        private BigDecimal cantidadAtendida;
        private BigDecimal cantidadSolicitada;
        private EstadoSolicitudMovimientoDetalle estadoDetalle;
    }
}

