package com.willyes.clemenintegra.documental.dto;

import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import jakarta.validation.constraints.NotNull;

public class DocumentoEstadoRequest {
    @NotNull
    public EstadoDocumento estado;
}
