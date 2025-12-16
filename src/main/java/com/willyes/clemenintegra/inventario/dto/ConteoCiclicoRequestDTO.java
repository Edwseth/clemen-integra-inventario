package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConteoCiclicoRequestDTO {

    @NotNull
    private Integer almacenId;
}
