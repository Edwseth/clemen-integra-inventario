package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroRequestDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;

import java.util.List;

public interface ResultadoAnalisisMicroService {
    List<ResultadoAnalisisMicroResponseDTO> guardarResultados(Long evaluacionId, List<ResultadoAnalisisMicroRequestDTO> payload);

    List<ResultadoAnalisisMicroResponseDTO> obtenerPorEvaluacion(Long evaluacionId);

    byte[] obtenerPdfMicro(Long evaluacionId);
}

