package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class DetalleEtapaRequest {
    @NotNull
    @PastOrPresent
    public LocalDateTime fechaInicio;

    @FutureOrPresent
    public LocalDateTime fechaFin;

    @Size(max = 255)
    public String observaciones;

    @NotNull
    public Long etapaProduccionId;

    @NotNull
    public Long ordenProduccionId;

    @NotNull
    public Long operarioId;
}