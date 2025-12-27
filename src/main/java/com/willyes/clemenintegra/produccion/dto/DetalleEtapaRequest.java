package com.willyes.clemenintegra.produccion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class DetalleEtapaRequest {
    @PastOrPresent
    public LocalDateTime fechaInicio;

    @FutureOrPresent
    public LocalDateTime fechaFin;

    @Size(max = 255)
    public String observaciones;

    @NotNull
    @JsonAlias("ordenProduccionEtapaId")
    public Long etapaProduccionId;

    @NotNull
    public Long ordenProduccionId;

    public Long operarioId;

    /**
     * Campo opcional que puede llegar desde el frontend pero no se usa para guardar.
     */
    @JsonAlias("operario")
    public String operarioNombre;
}
