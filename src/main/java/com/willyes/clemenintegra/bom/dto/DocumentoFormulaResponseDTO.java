package com.willyes.clemenintegra.bom.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoFormulaResponseDTO {
    private Long id;
    private String tipoDocumento;
    private String rutaArchivo;
}