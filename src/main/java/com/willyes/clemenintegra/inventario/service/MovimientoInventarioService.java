package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimientoInventarioService {
    MovimientoInventarioDTO registrarMovimiento(MovimientoInventarioDTO dto);

    Page<MovimientoInventario> consultarMovimientosConFiltros(MovimientoInventarioFiltroDTO filtro, Pageable pageable);

}

