package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AlmacenResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AlmacenService {
    Page<AlmacenResponseDTO> buscarPorTipoYCategoria(TipoAlmacen tipo, TipoCategoria categoria, Pageable pageable);
}
