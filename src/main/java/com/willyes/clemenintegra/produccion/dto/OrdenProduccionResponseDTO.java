package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;

public class OrdenProduccionResponseDTO {
    public Long id;
    public String loteProduccion;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public Integer cantidadProgramada;
    public Integer cantidadProducida;
    public String estado;
    public String nombreProducto;
    public String nombreResponsable;
}