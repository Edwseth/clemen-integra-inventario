package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RetencionLoteService {
    Page<RetencionLoteDTO> listar(EstadoRetencion estado, Pageable pageable);
    RetencionLoteDTO crear(RetencionLoteDTO dto);
    RetencionLoteDTO actualizar(Long id, RetencionLoteDTO dto);
    RetencionLoteDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
