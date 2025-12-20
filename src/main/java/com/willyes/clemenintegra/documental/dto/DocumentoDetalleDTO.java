package com.willyes.clemenintegra.documental.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentoDetalleDTO {

    private DocumentoDTO documento;
    private List<DocumentoVersionDTO> versiones;
}
