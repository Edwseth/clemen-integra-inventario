package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CapaService {
    Page<CapaDTO> listar(EstadoCapa estado, SeveridadNoConformidad severidad, Pageable pageable);
    CapaDTO crear(CapaDTO dto);
    CapaDTO actualizar(Long id, CapaDTO dto);
    CapaDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
