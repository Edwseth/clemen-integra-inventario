package com.willyes.clemenintegra.documental.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentoVersionCreateRequest {

    private String nombreVisible;
    private String comentarios;
    private LocalDateTime fechaEmision;
}
