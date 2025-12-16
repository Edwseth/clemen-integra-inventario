package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UbicacionFisicaRequestDTO {

    @NotNull
    private Integer almacenId;

    @NotBlank
    @Size(max = 50)
    private String codigo;

    @Size(max = 150)
    private String descripcion;

    private Boolean activo;
}
