package com.willyes.clemenintegra.inventario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoRecepcionServiceImpl implements CodigoRecepcionService {

    private static final DateTimeFormatter PREFIJO_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public String generarCodigo(LocalDate fecha) {
        final int anio = fecha.getYear();
        final String prefijo = fecha.format(PREFIJO_FORMATTER); // yyyymmdd

        // 1) UPSERT idempotente para asegurar la fila del día
        jdbc.update("""
        INSERT INTO secuencias_recepcion (anio, prefijo, secuencia_actual, actualizado_en)
        VALUES (?, ?, 0, NOW())
        ON DUPLICATE KEY UPDATE actualizado_en = NOW()
    """, anio, prefijo);

        // 2) Incremento atómico + captura del valor nuevo en la conexión actual
        jdbc.update("""
        UPDATE secuencias_recepcion
           SET secuencia_actual = LAST_INSERT_ID(secuencia_actual + 1),
               actualizado_en = NOW()
         WHERE anio = ? AND prefijo = ?
    """, anio, prefijo);

        // 3) Lee el valor incrementado
        Long secuencia = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 4) Formatea tu código (ajusta a tu estándar)
        return "RC-" + prefijo + "-OC-" + String.format("%03d", secuencia);
    }

}
