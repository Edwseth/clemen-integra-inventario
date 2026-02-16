package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AjusteInventarioService {
    Page<AjusteInventarioResponseDTO> listar(Pageable pageable, LocalDate fechaInicio, LocalDate fechaFin, Long productoId, Long almacenId);
    AjusteInventarioResponseDTO crear(AjusteInventarioRequestDTO dto);
    void eliminar(Long id);
}
