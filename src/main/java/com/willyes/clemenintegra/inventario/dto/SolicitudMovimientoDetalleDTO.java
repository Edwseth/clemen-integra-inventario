package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoDetalleDTO {
    private Long loteId;
    private String codigoLote;
    private BigDecimal cantidad;
    private BigDecimal cantidadAtendida;
    private EstadoSolicitudMovimientoDetalle estado;
    private Integer almacenOrigenId;
    private String nombreAlmacenOrigen;
    private Integer almacenDestinoId;
    private String nombreAlmacenDestino;
}
