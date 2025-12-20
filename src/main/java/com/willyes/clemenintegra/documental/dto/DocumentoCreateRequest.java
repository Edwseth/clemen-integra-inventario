package com.willyes.clemenintegra.documental.dto;

import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentoCreateRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String nombre;

    @NotNull
    private TipoDocumento tipo;

    @NotNull
    private AreaDocumento area;

    private String descripcion;
}
