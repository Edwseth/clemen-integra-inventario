package com.willyes.clemenintegra.shared.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateParser {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private DateParser() {
    }

    public static LocalDateTime parseStart(String s) {
        return parse(s, true);
    }

    public static LocalDateTime parseEnd(String s) {
        return parse(s, false);
    }

    private static LocalDateTime parse(String s, boolean start) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Fecha requerida");
        }
        if (s.contains("Z") || s.contains("+")) {
            throw new IllegalArgumentException("No se permiten zonas horarias");
        }
        if (s.contains("T")) {
            String timePart = s.substring(s.indexOf('T') + 1);
            if (timePart.contains("-")) {
                throw new IllegalArgumentException("No se permiten zonas horarias");
            }
            try {
                return LocalDateTime.parse(s, DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Formato de fecha inválido");
            }
        } else {
            try {
                LocalDate d = LocalDate.parse(s, DATE);
                return start ? d.atStartOfDay() : d.atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                try {
                    LocalDate d = LocalDate.parse(s, DATE_SLASH);
                    return start ? d.atStartOfDay() : d.atTime(23, 59, 59);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Formato de fecha inválido");
                }
            }
        }
    }
}
