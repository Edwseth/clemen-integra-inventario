package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class SolicitudAlistamientoResumenDTO {
    private Long solicitudId;
    private String codigoSolicitud;
    private String estado;
    private LocalDateTime fechaCreacion;
    private String almacenOrigenNombre;
    private String almacenDestinoNombre;
    private Integer totalLineas;
}
