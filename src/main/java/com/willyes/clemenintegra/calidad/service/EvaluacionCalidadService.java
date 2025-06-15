package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;

import java.util.List;

public interface EvaluacionCalidadService {
    List<EvaluacionCalidadResponseDTO> listarTodos();
    EvaluacionCalidadResponseDTO crear(EvaluacionCalidadRequestDTO dto);
    EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto);
    EvaluacionCalidadResponseDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
