package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;

import java.util.List;

public interface TipoMovimientoDetalleService {
    List<TipoMovimientoDetalleDTO> listarTodos();
    TipoMovimientoDetalleDTO crear(TipoMovimientoDetalleDTO dto);
    void eliminarPorId(Long id);
}
