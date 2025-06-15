package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;

import java.util.List;

public interface CategoriaProductoService {
    List<CategoriaProductoResponseDTO> listarTodas();
    CategoriaProductoResponseDTO obtenerPorId(Long id);
    CategoriaProductoResponseDTO crear(CategoriaProductoRequestDTO dto);
    CategoriaProductoResponseDTO actualizar(Long id, CategoriaProductoRequestDTO dto);
    void eliminar(Long id);
}
