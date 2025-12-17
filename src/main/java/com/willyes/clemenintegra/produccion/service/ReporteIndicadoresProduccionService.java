package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;

import java.time.LocalDate;

public interface ReporteIndicadoresProduccionService {

    byte[] generarExcelIndicadores(IndicadoresProduccionResponseDTO indicadores,
                                   LocalDate fechaInicio,
                                   LocalDate fechaFin);
}

