package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;

public class ProduccionResponse {
    public Long id;
    public String codigoLote;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public String estado;
    public String usuarioNombre;
    public String productoNombre;
}
