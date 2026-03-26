package com.willyes.clemenintegra.inventario.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ConteoCiclicoDetalleResponseDTO {
    Long id;
    Long productoId;
    String productoNombre;
    String codigoSku;
    Long loteProductoId;
    String loteCodigo;
    Long ubicacionFisicaId;
    BigDecimal stockSistema;
    BigDecimal conteoFisico;
    BigDecimal diferencia;
}
