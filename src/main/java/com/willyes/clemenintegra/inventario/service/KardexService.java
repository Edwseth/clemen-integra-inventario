package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;

import java.util.List;

public interface KardexService {
    List<KardexItemDTO> obtenerKardex(KardexFiltro filtro);
}
