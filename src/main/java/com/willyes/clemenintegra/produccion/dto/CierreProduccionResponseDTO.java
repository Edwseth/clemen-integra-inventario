package com.willyes.clemenintegra.produccion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CierreProduccionResponseDTO {
    public Long id;
    public LocalDateTime fechaCierre;
    public BigDecimal cantidad;
    public String tipo;
    public Boolean cerradaIncompleta;
    public String turno;
    public String observacion;
    public String unidadMedidaSimbolo;
    public Long usuarioId;
    public String usuarioNombre;
}
