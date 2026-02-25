package com.willyes.clemenintegra.produccion.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosteoProduccionServiceTest {

    private final CosteoProduccionService service = new CosteoProduccionService();

    @Test
    void regularizacionPositiva_reduceCostoUnitario() {
        BigDecimal baseConsumo = new BigDecimal("1000.000000");
        BigDecimal cantidadRealMayor = new BigDecimal("120.000000");

        BigDecimal costo = service.calcularCostoUnitarioMaterialOp(baseConsumo, cantidadRealMayor);

        assertEquals(new BigDecimal("8.333333"), costo);
    }

    @Test
    void regularizacionNegativa_incrementaCostoUnitario() {
        BigDecimal baseConsumo = new BigDecimal("1000.000000");
        BigDecimal cantidadRealMenor = new BigDecimal("80.000000");

        BigDecimal costo = service.calcularCostoUnitarioMaterialOp(baseConsumo, cantidadRealMenor);

        assertEquals(new BigDecimal("12.500000"), costo);
    }
}
