package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChecklistItemDTO {
    private Long id;
    public String nombrePaso;
    public Boolean obligatorio;
    public Boolean completado;
    public Boolean noAplica;
    public Boolean permitirNoAplica;
    public String estado;
    public String observacion;
    public LocalDateTime completedAt;
    public String completedByNombre;
}
