package com.willyes.clemenintegra.calidad.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentoCalidadVersionDownloadDTO {

    private String nombreArchivo;
    private String contentType;
    private byte[] contenido;
}
