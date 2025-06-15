package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;

import java.util.List;

public interface CapaService {
    List<CapaDTO> listarTodos();
    CapaDTO crear(CapaDTO dto);
    CapaDTO actualizar(Long id, CapaDTO dto);
    CapaDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
