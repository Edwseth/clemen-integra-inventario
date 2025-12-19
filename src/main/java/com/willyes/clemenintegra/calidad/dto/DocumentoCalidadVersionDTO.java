package com.willyes.clemenintegra.calidad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoCalidadVersionDTO {

    private Long id;
    private Long documentoId;
    private Integer version;
    private String nombreArchivo;
    private String nombreVisible;
    private String contentType;
    private Long sizeBytes;
    private String storagePath;
    private String hashOpcional;
    private Long creadoPorId;
    private LocalDateTime fechaCreacion;
}
