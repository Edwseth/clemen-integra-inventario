package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaAnalisisMicroDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private Integer version;
    private List<ParametroAnalisisMicroDTO> parametros;
    private boolean requiereAnalisisMicro;
}

