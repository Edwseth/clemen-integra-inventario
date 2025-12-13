package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoResultadoAnalisis;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ResultadoMicroValidador {

    private static final Pattern OPERADOR_NUMERICO = Pattern.compile("(?i)(<=|>=|<|>|=)?\\s*([0-9]+(?:\\.[0-9]+)?)");

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

        String operador = matcher.group(1) == null ? "=" : matcher.group(1);
        BigDecimal limite;
        try {
            limite = new BigDecimal(matcher.group(2));
        } catch (NumberFormatException e) {
            return cumpleActual;
        }

        BigDecimal valorResultado;
        try {
            valorResultado = new BigDecimal(resultado.trim());
        } catch (Exception e) {
            return cumpleActual;
        }

        int comparacion = valorResultado.compareTo(limite);
        return switch (operador) {
            case "<" -> comparacion < 0;
            case "<=" -> comparacion <= 0;
            case ">" -> comparacion > 0;
            case ">=" -> comparacion >= 0;
            default -> comparacion == 0;
        };
    }

    private static String extraerNumero(String texto) {
        Matcher matcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(texto);
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

