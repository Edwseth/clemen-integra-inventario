package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryCatalogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class UmValidator {

    private final InventoryCatalogProperties properties;

    /**
     * Ajusta la cantidad a los decimales permitidos según configuración.
     * Si excede el máximo, se redondea utilizando el modo configurado.
     * Si está por debajo del mínimo, se rellena con ceros.
     */
    public BigDecimal ajustar(BigDecimal cantidad) {
        if (cantidad == null) {
            return null;
        }
        int min = properties.getUm().getDecimales().getMin();
        int max = properties.getUm().getDecimales().getMax();
        RoundingMode mode = getRoundingMode();
        BigDecimal result = cantidad;
        if (result.scale() > max) {
            result = result.setScale(max, mode);
        }
        if (result.scale() < min) {
            result = result.setScale(min, mode);
        }
        return result;
    }

    public RoundingMode getRoundingMode() {
        return RoundingMode.valueOf(properties.getUm().getRedondeo());
    }
}
