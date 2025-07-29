package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;

import java.time.LocalDateTime;
import java.util.List;

public interface SolicitudMovimientoService {
    SolicitudMovimientoResponseDTO registrarSolicitud(SolicitudMovimientoRequestDTO dto);
    List<SolicitudMovimientoResponseDTO> listarSolicitudes(EstadoSolicitudMovimiento estado, LocalDateTime desde, LocalDateTime hasta);
    SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId);
    SolicitudMovimientoResponseDTO rechazarSolicitud(Long id, Long responsableId, String observaciones);
}
