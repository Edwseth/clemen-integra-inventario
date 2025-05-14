package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;

public class ProduccionRequest {
    public String codigoLote;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public String estado;
    public Long usuarioId;
    public Long productoId;
}