package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface EvaluacionCalidadService {
    Page<EvaluacionCalidadResponseDTO> listar(ResultadoEvaluacion resultado, Pageable pageable);
    EvaluacionCalidadResponseDTO crear(EvaluacionCalidadRequestDTO dto, MultipartFile archivo);
    EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto);
    EvaluacionCalidadResponseDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
