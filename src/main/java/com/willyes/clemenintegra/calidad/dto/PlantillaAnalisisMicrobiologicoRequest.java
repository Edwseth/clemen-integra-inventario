package com.willyes.clemenintegra.calidad.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaAnalisisMicrobiologicoRequest {
    private String nombre;
    private String descripcion;
    private LocalDate fechaVigenciaDesde;
    private LocalDate fechaVigenciaHasta;
    private Boolean vigente;
    private Long clonarDesdeId;
    private List<ParametroAnalisisMicrobiologicoDTO> parametros;
}
