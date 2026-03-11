package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaMicrobiologicaReutilizableDTO {
    private Long id;
    private String nombre;
    private String productoNombre;
    private Integer version;
    private Integer numeroParametros;
}

