package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudesPorOrdenDTO {
    private Long ordenProduccionId;
    private String codigoOrden;
    private LocalDateTime fechaCreacionOrden;
    private String solicitanteOrden;
    private List<SolicitudMovimientoItemDTO> items;
}
