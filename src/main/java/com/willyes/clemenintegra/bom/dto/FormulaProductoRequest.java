package com.willyes.clemenintegra.bom.dto;

import java.time.LocalDateTime;

public class FormulaProductoRequest {
    public Long productoId;
    public String version;
    public String estado;
    public LocalDateTime fechaCreacion;
    public Long creadoPorId;
}
