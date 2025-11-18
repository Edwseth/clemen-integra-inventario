package com.willyes.clemenintegra.calidad.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class VidaUtilProductoDTO {
    Integer productoId;
    String codigoSku;
    String nombreProducto;
    Integer semanasVigencia;
    String actualizadoPorNombre;
    LocalDateTime fechaActualizacion;
}

