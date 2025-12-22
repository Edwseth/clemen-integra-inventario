package com.willyes.clemenintegra.documental.dto;

import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private TipoDocumento tipo;
    private AreaDocumento area;
    private EstadoDocumento estado;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private String creadoPorNombre;
    private Integer numeroVersionVigente;
    private LocalDateTime fechaEmisionVersionVigente;
    private Long versionVigenteId;
}
