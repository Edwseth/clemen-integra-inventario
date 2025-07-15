package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;

import java.util.List;

public interface AjusteInventarioService {
    List<AjusteInventarioResponseDTO> listar();
    AjusteInventarioResponseDTO crear(AjusteInventarioRequestDTO dto, Long usuarioId);
    void eliminar(Long id);
}
