package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;

import java.util.List;

public interface RetencionLoteService {
    List<RetencionLoteDTO> listarTodos();
    RetencionLoteDTO crear(RetencionLoteDTO dto);
    RetencionLoteDTO actualizar(Long id, RetencionLoteDTO dto);
    RetencionLoteDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
