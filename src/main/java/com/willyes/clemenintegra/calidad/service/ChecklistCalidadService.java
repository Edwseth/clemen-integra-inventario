package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ChecklistCalidadDTO;
import com.willyes.clemenintegra.calidad.model.enums.TipoChecklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChecklistCalidadService {
    Page<ChecklistCalidadDTO> listar(TipoChecklist tipo, Pageable pageable);
    ChecklistCalidadDTO crear(ChecklistCalidadDTO dto);
    ChecklistCalidadDTO actualizar(Long id, ChecklistCalidadDTO dto);
    ChecklistCalidadDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
