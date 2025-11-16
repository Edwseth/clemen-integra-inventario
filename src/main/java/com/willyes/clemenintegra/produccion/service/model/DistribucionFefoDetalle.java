package com.willyes.clemenintegra.produccion.service.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DistribucionFefoDetalle {
    private final Long loteProductoId;
    private final String codigoLote;
    private final Long almacenId;
    private final BigDecimal cantidadCalculo;
    private final BigDecimal cantidadReserva;
    private final BigDecimal disponible;
    private final String estado;
}
