package com.willyes.clemenintegra.produccion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Servicio utilitario para convertir cantidades entre unidades básicas.
 * Solo maneja conversiones simples entre pares comunes (g↔kg, ml↔l).
 */
@Service
public class UnidadConversionService {

    public BigDecimal convertir(BigDecimal cantidad, String origen, String destino) {
        if (cantidad == null || origen == null || destino == null) {
            return cantidad;
        }
        if (origen.equalsIgnoreCase(destino)) {
            return cantidad;
        }
        // Masa
        if (origen.equalsIgnoreCase("g") && destino.equalsIgnoreCase("kg")) {
            return cantidad.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        }
        if (origen.equalsIgnoreCase("kg") && destino.equalsIgnoreCase("g")) {
            return cantidad.multiply(BigDecimal.valueOf(1000));
        }
        // Volumen
        if (origen.equalsIgnoreCase("ml") && destino.equalsIgnoreCase("l")) {
            return cantidad.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        }
        if (origen.equalsIgnoreCase("l") && destino.equalsIgnoreCase("ml")) {
            return cantidad.multiply(BigDecimal.valueOf(1000));
        }
        // Sin conversión conocida
        return cantidad;
    }
}
