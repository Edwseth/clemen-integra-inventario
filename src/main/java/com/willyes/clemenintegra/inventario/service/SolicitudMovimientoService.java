package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SolicitudMovimientoService {
    SolicitudMovimientoResponseDTO registrarSolicitud(SolicitudMovimientoRequestDTO dto);
    Page<SolicitudMovimientoListadoDTO> listarSolicitudes(EstadoSolicitudMovimiento estado,
                                                          String busqueda,
                                                          Long almacenOrigenId,
                                                          Long almacenDestinoId,
                                                          LocalDate desde,
                                                          LocalDate hasta,
                                                          Pageable pageable);
    SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId);
    SolicitudMovimientoResponseDTO rechazarSolicitud(Long id, Long responsableId, String observaciones);
    SolicitudMovimientoResponseDTO revertirAutorizacion(Long id, Long responsableId);

    Page<SolicitudesPorOrdenDTO> listGroupByOrden(EstadoSolicitudMovimiento estado,
                                                  LocalDate desde,
                                                  LocalDate hasta,
                                                  Pageable pageable);

    SolicitudesPorOrdenDTO obtenerPorOrden(Long ordenId);

    PicklistDTO generarPicklist(Long ordenId, boolean incluirAprobadas);
}
