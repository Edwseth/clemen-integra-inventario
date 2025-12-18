package com.willyes.clemenintegra.calidad.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CapaArchivoDescargaDTO {
    byte[] contenido;
    String nombreArchivo;
    String contentType;
}
