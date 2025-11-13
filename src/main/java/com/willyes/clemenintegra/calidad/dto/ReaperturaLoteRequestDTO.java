package com.willyes.clemenintegra.calidad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaperturaLoteRequestDTO {

    @NotBlank
    private String motivo;

    private String comentarios;
}
