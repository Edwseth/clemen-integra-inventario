package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AlistamientoOrdenProduccionDTO {
    private Long ordenId;
    private String codigoOrden;
    private String estadoOrden;
    private String estadoAlistamiento;
    private List<SolicitudAlistamientoResumenDTO> solicitudes;
}
