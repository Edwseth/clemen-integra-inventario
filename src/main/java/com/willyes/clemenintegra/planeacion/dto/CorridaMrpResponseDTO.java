package com.willyes.clemenintegra.planeacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorridaMrpResponseDTO {
    private Long id;
    private LocalDateTime fechaEjecucion;
    private LocalDate horizonteDesde;
    private LocalDate horizonteHasta;
    private String modo;
    private Long usuarioEjecutoId;
    private String resumen;
    private Integer totalSugerencias;
    private List<SugerenciaAbastecimientoResponseDTO> sugerencias;
}
