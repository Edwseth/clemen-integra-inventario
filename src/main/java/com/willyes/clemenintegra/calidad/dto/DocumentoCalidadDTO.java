package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoCalidadDTO {

    private Long id;
    private DocumentoCalidadTipo tipo;
    private String codigo;
    private String nombre;
    private DocumentoCalidadEstado estado;
    private Long loteId;
    private Long creadoPorId;
    private LocalDateTime fechaCreacion;
    private Long actualizadoPorId;
    private LocalDateTime fechaActualizacion;
}
