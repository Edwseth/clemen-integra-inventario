package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.time.LocalDate;

public interface EvaluacionCalidadService {
    Page<EvaluacionCalidadResponseDTO> listar(ResultadoEvaluacion resultado, Pageable pageable);
    Page<EvaluacionCalidadResponseDTO> listarPorFecha(LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable);
    EvaluacionCalidadResponseDTO crear(EvaluacionCalidadRequestDTO dto, List<MultipartFile> archivos);
    EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto);
    EvaluacionCalidadResponseDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
