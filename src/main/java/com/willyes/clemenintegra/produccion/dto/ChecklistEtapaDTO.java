package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChecklistEtapaDTO {
    private Long etapaId;
    private Long ordenProduccionId;
    private List<ChecklistItemDTO> items;
    private Boolean completo;
    private Integer faltantesObligatorios;
}
