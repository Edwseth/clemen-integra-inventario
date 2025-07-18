package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.EspecificacionCalidadDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EspecificacionCalidadService {
    Page<EspecificacionCalidadDTO> listar(Long productoId, Pageable pageable);
    EspecificacionCalidadDTO crear(EspecificacionCalidadDTO dto);
    EspecificacionCalidadDTO actualizar(Long id, EspecificacionCalidadDTO dto);
    EspecificacionCalidadDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
