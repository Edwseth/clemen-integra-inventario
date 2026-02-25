package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosteoInventarioServiceImplTest {

    private final CosteoInventarioService service = new CosteoInventarioServiceImpl();

    @Test
    void calcularCostoUnitarioRecepcion_conIvaCapitalizado() {
        OrdenCompraDetalle det = OrdenCompraDetalle.builder()
                .valorUnitario(BigDecimal.valueOf(100))
                .iva(BigDecimal.valueOf(19))
                .build();

        BigDecimal costoUnitario = service.calcularCostoUnitarioRecepcion(
                det,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1190)
        );

        assertEquals(new BigDecimal("119.000000"), costoUnitario);
    }

    @Test
    void calcularCostoUnitarioRecepcion_conGastosProrrateadosProporcionalmente() {
        OrdenCompraDetalle l1 = OrdenCompraDetalle.builder()
                .valorUnitario(BigDecimal.valueOf(100))
                .iva(BigDecimal.valueOf(19))
                .build();
        OrdenCompraDetalle l2 = OrdenCompraDetalle.builder()
                .valorUnitario(BigDecimal.valueOf(200))
                .iva(BigDecimal.valueOf(19))
                .build();

        BigDecimal cantidad = BigDecimal.ONE;
        BigDecimal gastos = BigDecimal.valueOf(100);
        BigDecimal subtotalTotalConIva = new BigDecimal("357.000000");

        BigDecimal costoTotalL1 = service.calcularCostoTotalLineaRecepcion(l1, cantidad, gastos, subtotalTotalConIva);
        BigDecimal costoTotalL2 = service.calcularCostoTotalLineaRecepcion(l2, cantidad, gastos, subtotalTotalConIva);

        assertEquals(new BigDecimal("152.333333"), costoTotalL1);
        assertEquals(new BigDecimal("304.666667"), costoTotalL2);
    }

    @Test
    void calcularCostoTotalMovimiento_consumoLote() {
        BigDecimal total = service.calcularCostoTotalMovimiento(new BigDecimal("50"), new BigDecimal("3"));
        assertEquals(new BigDecimal("150.000000"), total);
    }
}
