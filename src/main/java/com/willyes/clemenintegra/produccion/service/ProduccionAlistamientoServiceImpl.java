package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.produccion.dto.AlistamientoOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.SolicitudAlistamientoResumenDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProduccionAlistamientoServiceImpl implements ProduccionAlistamientoService {

    private static final Set<EstadoSolicitudMovimiento> ESTADOS_LISTO =
            EnumSet.of(EstadoSolicitudMovimiento.ATENDIDA, EstadoSolicitudMovimiento.CERRADA);
    private static final List<EstadoProduccion> ESTADOS_OP_CERRADAS = List.of(
            EstadoProduccion.FINALIZADA,
            EstadoProduccion.CANCELADA,
            EstadoProduccion.CERRADA_INCOMPLETA
    );

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final SolicitudMovimientoRepository solicitudMovimientoRepository;

    @Override
    public AlistamientoOrdenProduccionDTO obtenerAlistamientoPorOrden(Long ordenId) {
        OrdenProduccion orden = ordenProduccionRepository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Orden de producción no encontrada"));

        List<SolicitudMovimiento> solicitudes = solicitudMovimientoRepository.findWithDetalles(
                ordenId,
                null,
                null,
                null,
                false,
                ESTADOS_OP_CERRADAS
        );

        List<SolicitudAlistamientoResumenDTO> resumenes = solicitudes.stream()
                .map(this::toResumen)
                .toList();

        String estadoAlistamiento = determinarEstadoAlistamiento(solicitudes);

        return AlistamientoOrdenProduccionDTO.builder()
                .ordenId(orden.getId())
                .codigoOrden(orden.getCodigoOrden())
                .estadoOrden(orden.getEstado() != null ? orden.getEstado().name() : null)
                .estadoAlistamiento(estadoAlistamiento)
                .solicitudes(resumenes)
                .build();
    }

    private String determinarEstadoAlistamiento(List<SolicitudMovimiento> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            return "SIN_SOLICITUD";
        }
        boolean todasListas = solicitudes.stream()
                .map(SolicitudMovimiento::getEstado)
                .filter(Objects::nonNull)
                .allMatch(ESTADOS_LISTO::contains);
        if (todasListas) {
            return "LISTO";
        }
        return "EN_PROCESO";
    }

    private SolicitudAlistamientoResumenDTO toResumen(SolicitudMovimiento solicitud) {
        return SolicitudAlistamientoResumenDTO.builder()
                .solicitudId(solicitud.getId())
                .codigoSolicitud(solicitud.getId() != null ? solicitud.getId().toString() : null)
                .estado(solicitud.getEstado() != null ? solicitud.getEstado().name() : null)
                .fechaCreacion(solicitud.getFechaSolicitud())
                .almacenOrigenNombre(solicitud.getAlmacenOrigen() != null ? solicitud.getAlmacenOrigen().getNombre() : null)
                .almacenDestinoNombre(solicitud.getAlmacenDestino() != null ? solicitud.getAlmacenDestino().getNombre() : null)
                .totalLineas(solicitud.getDetalles() != null ? solicitud.getDetalles().size() : null)
                .build();
    }
}
