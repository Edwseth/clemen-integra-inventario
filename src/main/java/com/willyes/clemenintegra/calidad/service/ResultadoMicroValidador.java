package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ResultadoMicroValidador {

    private static final Pattern OPERADOR_NUMERICO =
            Pattern.compile("(<=|>=|<|>|=|≤|≥)\\s*([0-9]+(?:[.,][0-9]+)?)");
    private static final Pattern SOLO_NUMERO = Pattern.compile("([0-9][0-9.,]*)");
    private static final Pattern MILES_PUNTO = Pattern.compile("^\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?$");
    private static final Pattern MILES_COMA = Pattern.compile("^\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?$");

    private ResultadoMicroValidador() {
    }

    static Boolean calcularCumplimiento(ParametroAnalisisMicrobiologico parametro, String resultado, Boolean cumpleActual) {
        if (parametro == null || parametro.getTipoResultado() == null) {
            return cumpleActual;
        }

        return switch (parametro.getTipoResultado()) {
            case NUMERICO -> evaluarNumerico(parametro.getEspecificacion(), resultado, cumpleActual);
            case PRESENCIA_AUSENCIA -> evaluarPresenciaAusencia(parametro.getEspecificacion(), resultado, cumpleActual);
            case TEXTO -> cumpleActual;
        };
    }

    private static Boolean evaluarNumerico(String especificacion, String resultado, Boolean cumpleActual) {
        if (especificacion == null || resultado == null) {
            return cumpleActual;
        }

        Matcher matcher = OPERADOR_NUMERICO.matcher(especificacion);
        if (!matcher.find()) {
            String lower = especificacion.toLowerCase(Locale.ROOT);
            if (lower.contains("max")) {
                matcher = OPERADOR_NUMERICO.matcher("<= " + extraerNumero(lower));
            } else if (lower.contains("min")) {
                matcher = OPERADOR_NUMERICO.matcher(">= " + extraerNumero(lower));
            }
            if (matcher == null || !matcher.find()) {
                return cumpleActual;
            }
        }

        String operador = normalizarOperador(matcher.group(1));
        BigDecimal limite;
        try {
            limite = parseNumero(matcher.group(2));
        } catch (Exception e) {
            return cumpleActual;
        }

        BigDecimal valorResultado;
        try {
            valorResultado = parseNumero(resultado);
        } catch (Exception e) {
            return cumpleActual;
        }

        int comparacion = valorResultado.compareTo(limite);
        return switch (operador) {
            // En microbiología los resultados reportados como `<X` pueden aparecer como `X` en laboratorio y deben considerarse dentro del límite.
            case "<" -> comparacion <= 0;
            case "<=" -> comparacion <= 0;
            case ">" -> comparacion > 0;
            case ">=" -> comparacion >= 0;
            default -> comparacion == 0;
        };
    }

    private static String normalizarOperador(String operador) {
        if (operador == null || operador.isBlank()) {
            return "=";
        }
        return switch (operador.trim()) {
            case "≤" -> "<=";
            case "≥" -> ">=";
            default -> operador.trim();
        };
    }

    private static BigDecimal parseNumero(String texto) {
        Matcher matcher = SOLO_NUMERO.matcher(texto == null ? "" : texto);
        if (!matcher.find()) {
            throw new NumberFormatException("No se encontró número en el texto");
        }

        String valor = matcher.group(1);
        if (MILES_PUNTO.matcher(valor).matches()) {
            valor = valor.replace(".", "").replace(',', '.');
        } else if (MILES_COMA.matcher(valor).matches()) {
            valor = valor.replace(",", "");
        } else if (valor.contains(",") && !valor.contains(".")) {
            valor = valor.replace(',', '.');
        }

        return new BigDecimal(valor);
    }

    private static String extraerNumero(String texto) {
        Matcher matcher = SOLO_NUMERO.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static Boolean evaluarPresenciaAusencia(String especificacion, String resultado, Boolean cumpleActual) {
        if (especificacion == null || resultado == null) {
            return cumpleActual;
        }

        if (especificacion.toUpperCase(Locale.ROOT).contains("AUSENCIA")) {
            return "AUSENCIA".equalsIgnoreCase(resultado.trim());
        }
        return cumpleActual;
    }
}
