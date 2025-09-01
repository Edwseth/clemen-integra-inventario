package com.willyes.clemenintegra.produccion.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class OrdenProduccionResponseDTO {
    public Long id;
    public String codigoOrden;
    public String loteProduccion;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public BigDecimal cantidadProgramada;
    public BigDecimal cantidadProgramadaBase;
    public BigDecimal cantidadProducida;
    public BigDecimal cantidadProducidaAcumulada;
    public BigDecimal cantidadRestante;
    public LocalDateTime fechaUltimoCierre;
    public BigDecimal unidadesProducidas;
    public String estado;
    public String nombreProducto;
    public String unidadMedida;
    public String unidadMedidaSimbolo;
    public String unidadMedidaBaseSimbolo;
    public String nombreResponsable;
}
