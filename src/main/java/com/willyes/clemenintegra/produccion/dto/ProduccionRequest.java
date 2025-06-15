package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class ProduccionRequest {
    @NotBlank
    public String codigoLote;

    @NotNull
    @PastOrPresent
    public LocalDateTime fechaInicio;

    @NotNull
    @FutureOrPresent
    public LocalDateTime fechaFin;

    @NotBlank
    public String estado;

    @NotNull
    public Long usuarioId;

    @NotNull
    public Long productoId;
}