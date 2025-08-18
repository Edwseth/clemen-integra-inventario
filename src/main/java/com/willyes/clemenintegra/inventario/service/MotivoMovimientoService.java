package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MotivoMovimientoService {
    Page<MotivoMovimientoResponseDTO> listar(Pageable pageable);
}
