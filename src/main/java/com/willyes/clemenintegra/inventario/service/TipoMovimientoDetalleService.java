package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.TipoMovimientoDetalleDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TipoMovimientoDetalleService {
    Page<TipoMovimientoDetalleDTO> listarTodos(Pageable pageable);
    TipoMovimientoDetalleDTO crear(TipoMovimientoDetalleDTO dto);
    void eliminarPorId(Long id);
}
