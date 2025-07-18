package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoConformidadService {
    Page<NoConformidadDTO> listar(SeveridadNoConformidad severidad,
                                  OrigenNoConformidad origen,
                                  Pageable pageable);
    NoConformidadDTO crear(NoConformidadDTO dto);
    NoConformidadDTO actualizar(Long id, NoConformidadDTO dto);
    void eliminar(Long id);
    NoConformidadDTO obtenerPorId(Long id);
}
