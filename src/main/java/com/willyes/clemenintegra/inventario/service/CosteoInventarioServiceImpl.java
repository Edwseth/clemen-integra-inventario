package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CosteoInventarioServiceImpl implements CosteoInventarioService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    @Override
    public BigDecimal calcularCostoUnitarioRecepcion(OrdenCompraDetalle det,
                                                     BigDecimal cantidadRecibida,
                                                     BigDecimal gastosAdicionalesTotalRecepcion,
                                                     BigDecimal subtotalTotalRecepcionConIva) {
        BigDecimal cantidad = safe(cantidadRecibida);
        if (cantidad.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        BigDecimal costoTotalLinea = calcularCostoTotalLineaRecepcion(det, cantidadRecibida,
                gastosAdicionalesTotalRecepcion, subtotalTotalRecepcionConIva);
        return costoTotalLinea.divide(cantidad, 6, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calcularCostoTotalLineaRecepcion(OrdenCompraDetalle det,
                                                       BigDecimal cantidadRecibida,
                                                       BigDecimal gastosAdicionalesTotalRecepcion,
                                                       BigDecimal subtotalTotalRecepcionConIva) {
        BigDecimal cantidad = safe(cantidadRecibida);
        BigDecimal valorUnitario = safe(det != null ? det.getValorUnitario() : null);
        BigDecimal ivaPorcentaje = safe(det != null ? det.getIva() : null);

        BigDecimal subtotalLinea = cantidad.multiply(valorUnitario);
        BigDecimal ivaValorLinea = subtotalLinea.multiply(ivaPorcentaje)
                .divide(CIEN, 6, RoundingMode.HALF_UP);
        BigDecimal baseConIvaLinea = subtotalLinea.add(ivaValorLinea);

        BigDecimal gastos = safe(gastosAdicionalesTotalRecepcion);
        BigDecimal subtotalTotalConIva = safe(subtotalTotalRecepcionConIva);
        BigDecimal prorrateo = BigDecimal.ZERO;
        if (gastos.compareTo(BigDecimal.ZERO) > 0 && subtotalTotalConIva.compareTo(BigDecimal.ZERO) > 0) {
            prorrateo = baseConIvaLinea
                    .divide(subtotalTotalConIva, 12, RoundingMode.HALF_UP)
                    .multiply(gastos);
        }

        return baseConIvaLinea.add(prorrateo).setScale(6, RoundingMode.HALF_UP);
    }


    @Override
    public BigDecimal calcularCostoTotalMovimiento(BigDecimal costoUnitario, BigDecimal cantidadMovimiento) {
        BigDecimal costoUnit = safe(costoUnitario).setScale(6, RoundingMode.HALF_UP);
        BigDecimal cantidad = safe(cantidadMovimiento).abs().setScale(6, RoundingMode.HALF_UP);
        return cantidad.multiply(costoUnit).setScale(6, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calcularCostoUnitarioPromedioPorIngreso(BigDecimal costoTotalMaterialIngresado,
                                                              BigDecimal totalIngresadoMaterial) {
        BigDecimal costoTotal = safe(costoTotalMaterialIngresado).setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalIngresado = safe(totalIngresadoMaterial).setScale(6, RoundingMode.HALF_UP);
        if (totalIngresado.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return costoTotal.divide(totalIngresado, 6, RoundingMode.HALF_UP);
    }

        private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
