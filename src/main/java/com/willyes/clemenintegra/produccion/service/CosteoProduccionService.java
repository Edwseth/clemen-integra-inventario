package com.willyes.clemenintegra.produccion.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CosteoProduccionService {

    public BigDecimal calcularCostoUnitarioMaterialOp(BigDecimal costoTotalMaterialRealOp, BigDecimal cantidadRealProducida) {
        BigDecimal costoTotal = costoTotalMaterialRealOp == null ? BigDecimal.ZERO : costoTotalMaterialRealOp;
        BigDecimal cantidadReal = cantidadRealProducida == null ? BigDecimal.ZERO : cantidadRealProducida;
        if (cantidadReal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return costoTotal.divide(cantidadReal, 6, RoundingMode.HALF_UP);
    }
}
