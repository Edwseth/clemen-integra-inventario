package com.willyes.clemenintegra.produccion.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjecutarCorridaOpHomeopaticaRequestDTO {

    @NotNull
    private Long responsableId;

    @NotNull
    @FutureOrPresent
    private LocalDateTime fechaProgramada;

    @Size(max = 120)
    private String idempotencyKey;

    @Size(max = 500)
    private String observacion;
}
