package com.willyes.clemenintegra.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class DocumentoFormulaDescargaDTO {
    private final Resource recurso;
    private final String nombreArchivo;
    private final String contentType;
}
