package com.willyes.clemenintegra.planeacion.service;

import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.dto.MrpSimpleRequestDTO;
import com.willyes.clemenintegra.planeacion.dto.SugerenciaAbastecimientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PlaneacionMrpService {

    CorridaMrpResponseDTO ejecutarMrpSimple(MrpSimpleRequestDTO request, Long usuarioId);

    Page<CorridaMrpResponseDTO> listarCorridas(LocalDate fechaDesde, LocalDate fechaHasta, String modo, Pageable pageable);

    Page<SugerenciaAbastecimientoResponseDTO> listarSugerencias(Long corridaId, Long productoId, String estado, String tipo, Pageable pageable);

    Page<SugerenciaAbastecimientoResponseDTO> listarSugerenciasPorCorrida(Long corridaId, Pageable pageable);
}
