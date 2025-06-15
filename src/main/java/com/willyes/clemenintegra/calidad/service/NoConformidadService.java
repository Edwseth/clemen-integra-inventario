package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;

import java.util.List;

public interface NoConformidadService {
    List<NoConformidadDTO> listarTodos();
    NoConformidadDTO crear(NoConformidadDTO dto);
    NoConformidadDTO actualizar(Long id, NoConformidadDTO dto);
    void eliminar(Long id);
    NoConformidadDTO obtenerPorId(Long id);
}
