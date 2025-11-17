package com.willyes.clemenintegra.calidad.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VidaUtilProductoDTO {
    Integer productoId;
    String codigoSku;
    String nombreProducto;
    Integer semanasVigencia;
}

