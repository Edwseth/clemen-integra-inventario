package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;

import java.util.List;

public interface BitacoraCambiosInventarioService {
    List<BitacoraCambiosInventarioDTO> listar();
    BitacoraCambiosInventarioDTO crear(BitacoraCambiosInventarioDTO dto);
    void eliminar(Long id);
}
