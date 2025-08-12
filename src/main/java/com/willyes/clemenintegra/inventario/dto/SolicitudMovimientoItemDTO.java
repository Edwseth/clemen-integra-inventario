package com.willyes.clemenintegra.inventario.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoItemDTO {
    private Long solicitudId;
    private Long productoId;
    private String nombreProducto;
    private String codigoLote;
    private BigDecimal cantidadSolicitada;
    private String unidadMedida;
    private Long almacenOrigenId;
    private String nombreAlmacenOrigen;
    private Long almacenDestinoId;
    private String nombreAlmacenDestino;
    private EstadoSolicitudMovimiento estado;
    private LocalDateTime fechaSolicitud;
    private String usuarioSolicitante;
    private String observaciones;
}
