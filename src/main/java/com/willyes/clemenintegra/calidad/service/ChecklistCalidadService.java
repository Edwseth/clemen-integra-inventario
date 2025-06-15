package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;

import java.util.List;

public interface ChecklistCalidadService {
    List<ChecklistCalidadDTO> listarTodos();
    ChecklistCalidadDTO crear(ChecklistCalidadDTO dto);
    ChecklistCalidadDTO actualizar(Long id, ChecklistCalidadDTO dto);
    ChecklistCalidadDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
