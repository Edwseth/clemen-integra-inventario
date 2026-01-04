package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChecklistEtapaTemplateResponse {
    private Long id;
    private Long etapaPlantillaId;
    private String nombreItem;
    private Boolean obligatorio;
    private Boolean permitirNoAplica;
    private Integer orden;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
