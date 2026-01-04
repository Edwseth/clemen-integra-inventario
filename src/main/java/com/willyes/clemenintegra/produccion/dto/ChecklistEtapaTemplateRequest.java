package com.willyes.clemenintegra.produccion.dto;

import lombok.Data;

@Data
public class ChecklistEtapaTemplateRequest {
    public String nombreItem;
    public Boolean obligatorio;
    public Boolean permitirNoAplica;
    public Integer orden;
    public Boolean activo;
}
