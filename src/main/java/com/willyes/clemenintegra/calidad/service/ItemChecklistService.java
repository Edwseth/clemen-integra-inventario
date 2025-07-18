package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ItemChecklistDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemChecklistService {
    Page<ItemChecklistDTO> listar(Long checklistId, Pageable pageable);
    ItemChecklistDTO crear(ItemChecklistDTO dto);
    ItemChecklistDTO actualizar(Long id, ItemChecklistDTO dto);
    ItemChecklistDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
