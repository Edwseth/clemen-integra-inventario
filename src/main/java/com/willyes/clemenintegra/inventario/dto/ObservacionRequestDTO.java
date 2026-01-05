package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservacionRequestDTO {

    @NotBlank(message = "La observación es obligatoria")
    private String observacion;
}

