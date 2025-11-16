package com.willyes.clemenintegra.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoFormulaMetadataDTO {
    private String tipoDocumento;
    private String nombreVisible;
}
