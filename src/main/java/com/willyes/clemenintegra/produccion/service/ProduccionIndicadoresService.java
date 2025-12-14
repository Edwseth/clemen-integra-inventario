package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface ProduccionIndicadoresService {

    IndicadoresProduccionResponseDTO calcularIndicadores(LocalDate fechaInicio, LocalDate fechaFin);

    List<AlertaOrdenProduccionDTO> obtenerOrdenesConAlertas(LocalDate fechaReferencia, int diasVentana);
}
