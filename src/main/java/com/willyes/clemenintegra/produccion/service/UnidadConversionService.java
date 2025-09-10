package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.service.UmValidator;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio utilitario para convertir cantidades entre unidades básicas.
 * Solo maneja conversiones simples entre pares comunes (g↔kg, ml↔l).
 */
@Service
@RequiredArgsConstructor
public class UnidadConversionService {

    private final UmValidator umValidator;

    public BigDecimal convertir(BigDecimal cantidad, String origen, String destino) {
        if (cantidad == null || origen == null || destino == null) {
            return cantidad;
        }
        if (origen.equalsIgnoreCase(destino)) {
            return cantidad;
        }
        // Masa
        if (origen.equalsIgnoreCase("g") && destino.equalsIgnoreCase("kg")) {
            return cantidad.divide(BigDecimal.valueOf(1000), 6, umValidator.getRoundingMode());
        }
        if (origen.equalsIgnoreCase("kg") && destino.equalsIgnoreCase("g")) {
            return cantidad.multiply(BigDecimal.valueOf(1000));
        }
        // Volumen
        if (origen.equalsIgnoreCase("ml") && destino.equalsIgnoreCase("l")) {
            return cantidad.divide(BigDecimal.valueOf(1000), 6, umValidator.getRoundingMode());
        }
        if (origen.equalsIgnoreCase("l") && destino.equalsIgnoreCase("ml")) {
            return cantidad.multiply(BigDecimal.valueOf(1000));
        }
        // Sin conversión conocida
        return cantidad;
    }

    /**
     * Normaliza las unidades de ambas cantidades antes de dividirlas.
     * Si las unidades ya coinciden, se realiza la división directamente.
     */
    public BigDecimal dividirNormalizado(BigDecimal numerador, String unidadNumerador,
                                         BigDecimal divisor, String unidadDivisor) {
        BigDecimal cantidadNormalizada = convertir(numerador, unidadNumerador, unidadDivisor);
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cantidadNormalizada.divide(divisor, 6, umValidator.getRoundingMode());
    }
}
