package com.willyes.clemenintegra.bom.dto;

import jakarta.validation.constraints.*;
public class DocumentoFormulaRequest {
    @NotNull
    public Long formulaId;

    @NotBlank
    public String tipoDocumento;

    @NotBlank
    public String rutaArchivo;
}
