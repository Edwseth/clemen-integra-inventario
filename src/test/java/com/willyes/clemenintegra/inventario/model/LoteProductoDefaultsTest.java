package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoteProductoDefaultsTest {

    @Test
    void loteBuilderDefaultsSeAplican() {
        LoteProducto lote = LoteProducto.builder()
                .codigoLote("L1")
                .estado(EstadoLote.DISPONIBLE)
                .stockLote(new BigDecimal("10.129"))
                .build();

        assertFalse(lote.isAgotado());
        assertEquals(new BigDecimal("0.000000"), lote.getStockReservado());

        ReflectionTestUtils.invokeMethod(lote, "normalizeDefaults");

        assertEquals(new BigDecimal("10.12"), lote.getStockLote());
        assertEquals(new BigDecimal("0.000000"), lote.getStockReservado());
    }
}

