package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoCalidadResumenDTO {

    private Long documentoId;
    private DocumentoCalidadTipo tipo;
    private String codigo;
    private String nombre;
    private Integer version;
    private String nombreVisible;
    private LocalDateTime fechaVersion;
}
