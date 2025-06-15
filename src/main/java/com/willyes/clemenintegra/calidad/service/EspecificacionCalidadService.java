package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EspecificacionCalidadDTO;

import java.util.List;

public interface EspecificacionCalidadService {
    List<EspecificacionCalidadDTO> listarTodos();
    EspecificacionCalidadDTO crear(EspecificacionCalidadDTO dto);
    EspecificacionCalidadDTO actualizar(Long id, EspecificacionCalidadDTO dto);
    EspecificacionCalidadDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
