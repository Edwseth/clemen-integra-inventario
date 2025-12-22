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
public class ResumenAlertasCalidadDTO {

    private List<AlertaLoteCalidadDTO> lotesProximosVencer;
    private List<AlertaLoteCalidadDTO> lotesVencidos;
    private List<AlertaLoteCalidadDTO> lotesPendientesLiberar;
    private Integer totalProximosVencer;
    private Integer totalVencidos;
    private Integer totalPendientesLiberar;
}
