package com.willyes.clemenintegra.calidad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapaArchivoDTO {
    private Long id;
    private String nombreArchivo;
    private String nombreVisible;
    private String contentType;
    private Long tamanoBytes;
    private LocalDateTime fechaCreacion;
}
