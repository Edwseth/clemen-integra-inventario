package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.UnidadMedidaRequestDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;

import java.util.List;

public interface UnidadMedidaService {
    List<UnidadMedidaResponseDTO> listarTodas();
    UnidadMedidaResponseDTO obtenerPorId(Long id);
    UnidadMedidaResponseDTO crear(UnidadMedidaRequestDTO dto);
    UnidadMedidaResponseDTO actualizar(Long id, UnidadMedidaRequestDTO dto);
    void eliminar(Long id);
}
