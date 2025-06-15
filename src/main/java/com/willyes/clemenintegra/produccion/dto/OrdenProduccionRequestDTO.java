package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenProduccionRequestDTO {
    @NotBlank
    private String loteProduccion;

    @NotNull
    @PastOrPresent
    private LocalDateTime fechaInicio;

    @NotNull
    @FutureOrPresent
    private LocalDateTime fechaFin;

    @NotNull
    @Min(1)
    private Integer cantidadProgramada;

    @Min(0)
    private Integer cantidadProducida;

    @NotBlank
    private String estado;

    @NotNull
    private Long productoId;

    @NotNull
    private Long responsableId;
}
