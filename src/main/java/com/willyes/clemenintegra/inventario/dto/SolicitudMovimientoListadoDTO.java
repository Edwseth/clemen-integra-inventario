package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudMovimientoListadoDTO {
    private Long id;
    private String op;
    private LocalDateTime fechaSolicitud;
    private Integer items;
    private Integer itemsCount;
    private String estado;
    private String solicitante;
}
