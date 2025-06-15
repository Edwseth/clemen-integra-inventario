package com.willyes.clemenintegra.bom.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoFormulaRequest {

    @NotNull
    private Long formulaId;

    @NotBlank
    private String tipoDocumento;

    @NotBlank
    private String rutaArchivo;
}
