package com.willyes.clemenintegra.inventario.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class HistorialEstadoOrdenRequest {
    @NotNull
    public Long ordenCompraId;

    @NotBlank
    @Size(max = 50)
    public String estado;

    @PastOrPresent
    public LocalDateTime fechaCambio;

    @NotNull
    public Long usuarioId;
    public String observaciones;
}
