package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;

public class OrdenProduccionRequest {
    public String loteProduccion;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public Integer cantidadProgramada;
    public Integer cantidadProducida;
    public String estado;
    public Long productoId;
    public Long responsableId;
}
