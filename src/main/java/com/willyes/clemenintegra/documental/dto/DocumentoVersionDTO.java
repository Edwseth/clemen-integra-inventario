package com.willyes.clemenintegra.documental.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoVersionDTO {

    private Long id;
    private Integer numeroVersion;
    private LocalDateTime fechaEmision;
    private String nombreVisible;
    private String nombreArchivoOriginal;
    private boolean vigente;
    private String emitidoPorNombre;
    private String comentarios;
    private Long tamanoBytes;
    private String contentType;
}
