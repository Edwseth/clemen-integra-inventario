package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SolicitudMovimientoService {
    SolicitudMovimientoResponseDTO registrarSolicitud(SolicitudMovimientoRequestDTO dto);
    List<SolicitudMovimientoResponseDTO> listarSolicitudes(EstadoSolicitudMovimiento estado, LocalDateTime desde, LocalDateTime hasta);
    SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId);
    SolicitudMovimientoResponseDTO rechazarSolicitud(Long id, Long responsableId, String observaciones);

    Page<SolicitudesPorOrdenDTO> listGroupByOrden(EstadoSolicitudMovimiento estado,
                                                  LocalDateTime desde,
                                                  LocalDateTime hasta,
                                                  Pageable pageable);

    PicklistDTO generarPicklist(Long ordenId, boolean incluirAprobadas);
}
