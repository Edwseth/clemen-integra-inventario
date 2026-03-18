package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KardexService {
    List<KardexItemDTO> obtenerKardex(KardexFiltro filtro);

    Page<KardexItemDTO> obtenerKardex(KardexFiltro filtro, Pageable pageable);
}
