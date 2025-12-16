package com.willyes.clemenintegra.inventario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConteoCiclicoUpdateRequestDTO {

    @Valid
    @NotEmpty(message = "Debe enviar al menos un detalle")
    private List<ConteoCiclicoDetalleRequestDTO> detalles;
}
