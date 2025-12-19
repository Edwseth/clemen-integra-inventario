package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentoCalidadCreateRequest {

    @NotNull
    private DocumentoCalidadTipo tipo;

    private String codigo;

    @NotBlank
    private String nombre;

    private Long loteId;
}
