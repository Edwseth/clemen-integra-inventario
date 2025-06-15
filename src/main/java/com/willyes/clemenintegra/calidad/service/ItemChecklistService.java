package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ItemChecklistDTO;

import java.util.List;

public interface ItemChecklistService {
    List<ItemChecklistDTO> listarTodos();
    ItemChecklistDTO crear(ItemChecklistDTO dto);
    ItemChecklistDTO actualizar(Long id, ItemChecklistDTO dto);
    ItemChecklistDTO obtenerPorId(Long id);
    void eliminar(Long id);
}
