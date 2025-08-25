package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudesPorOrdenDTO {
    private Long ordenProduccionId;
    private String codigoOrden;
    private String estadoAgregado;
    private String solicitanteOrden;
    private LocalDateTime fechaOrden;
    private List<SolicitudMovimientoItemDTO> items;
    private Integer itemsCount;

    public List<SolicitudMovimientoItemDTO> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    @JsonProperty("op")
    public String getOp() {
        return codigoOrden;
    }
}
