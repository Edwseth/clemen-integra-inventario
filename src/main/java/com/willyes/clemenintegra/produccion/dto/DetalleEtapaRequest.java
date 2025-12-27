package com.willyes.clemenintegra.produccion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class DetalleEtapaRequest {
    /**
     * La fecha de inicio se establece en backend usando la hora del servidor para evitar
     * desfaces por zona horaria.
     */
    public LocalDateTime fechaInicio;

    public LocalDateTime fechaFin;

    @Size(max = 255)
    public String observaciones;

    @NotNull
    @JsonAlias("ordenProduccionEtapaId")
    public Long etapaProduccionId;

    @NotNull
    public Long ordenProduccionId;

    /**
     * Se asigna en backend con el usuario autenticado para mantener la trazabilidad.
     */
    public Long operarioId;

    /**
     * Campo opcional que puede llegar desde el frontend pero no se usa para guardar.
     */
    @JsonAlias("operario")
    public String operarioNombre;
}
