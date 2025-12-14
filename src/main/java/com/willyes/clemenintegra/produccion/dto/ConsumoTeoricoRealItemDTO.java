package com.willyes.clemenintegra.produccion.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConsumoTeoricoRealItemDTO {

    private Long productoId;
    private String codigoSku;
    private String nombreProducto;
    private String unidad;

    private BigDecimal cantidadTeorica;
    private BigDecimal cantidadReal;
    private BigDecimal diferencia;
    private BigDecimal porcentajeDesviacion;
}

