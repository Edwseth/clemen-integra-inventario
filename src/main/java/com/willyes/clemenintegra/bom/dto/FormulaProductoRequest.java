package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

public class FormulaProductoRequest {
    @NotNull
    public Long productoId;

    @NotBlank
    public String version;

    @NotBlank
    public String estado;

    @PastOrPresent
    public LocalDateTime fechaCreacion;

    @NotNull
    public Long creadoPorId;
}
