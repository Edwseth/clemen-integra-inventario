package com.willyes.clemenintegra.produccion.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenProduccionRequestDTO {
    private String loteProduccion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer cantidadProgramada;
    private Integer cantidadProducida;
    private String estado;
    private Long productoId;
    private Long responsableId;
}
