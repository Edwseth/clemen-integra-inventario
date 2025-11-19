package com.willyes.clemenintegra.shared.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Permite deserializar fechas provenientes del frontend que puedan traer zona horaria o fracciones.
 * Siempre se normaliza a LocalDateTime ignorando la zona horaria explícita.
 */
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(trimmed, ISO_LOCAL);
        } catch (DateTimeParseException ignored) {
            // probar con formatos que incluyan zona horaria
        }
        try {
            OffsetDateTime offset = OffsetDateTime.parse(trimmed);
            return offset.toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // intentar como instante absoluto (terminado en Z)
        }
        try {
            Instant instant = Instant.parse(trimmed);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // dejar caer en excepción genérica
        }
        throw InvalidFormatException.from(p, "Formato de fecha inválido", trimmed, LocalDateTime.class);
    }
}
