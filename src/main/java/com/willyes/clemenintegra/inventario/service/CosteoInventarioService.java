package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;

import java.math.BigDecimal;

public interface CosteoInventarioService {

    BigDecimal calcularCostoUnitarioRecepcion(
            OrdenCompraDetalle det,
            BigDecimal cantidadRecibida,
            BigDecimal gastosAdicionalesTotalRecepcion,
            BigDecimal subtotalTotalRecepcionConIva
    );

    BigDecimal calcularCostoTotalLineaRecepcion(
            OrdenCompraDetalle det,
            BigDecimal cantidadRecibida,
            BigDecimal gastosAdicionalesTotalRecepcion,
            BigDecimal subtotalTotalRecepcionConIva
    );

    BigDecimal calcularCostoTotalMovimiento(BigDecimal costoUnitario, BigDecimal cantidadMovimiento);

    BigDecimal calcularCostoUnitarioPromedioPorIngreso(BigDecimal costoTotalMaterialIngresado,
                                                       BigDecimal totalIngresadoMaterial);
}
