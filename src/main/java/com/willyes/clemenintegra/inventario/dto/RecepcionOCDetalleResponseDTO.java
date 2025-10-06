package com.willyes.clemenintegra.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionOCDetalleResponseDTO {

    private Long id;
    private Long ordenCompraDetalleId;
    private Integer productoId;
    private String productoNombre;
    private String productoSku;
    private Long loteId;
    private String codigoLote;
    private BigDecimal cantidadRecibida;
}

