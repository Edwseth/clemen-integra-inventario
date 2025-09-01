package com.willyes.clemenintegra.produccion.mapper;

import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import java.math.BigDecimal;

public class ProduccionMapper {

    public static OrdenProduccion toEntity(OrdenProduccionRequestDTO dto, Producto producto, Usuario responsable) {
        return OrdenProduccion.builder()
                .loteProduccion(dto.getLoteProduccion())
                // La fecha de inicio se establecerá desde el servicio, ignorando valores del cliente
                .fechaFin(dto.getFechaFin())
                .cantidadProgramada(dto.getCantidadProgramada())
                .cantidadProducida(dto.getCantidadProducida())
                .estado(EstadoProduccion.valueOf(dto.getEstado()))
                .producto(producto)
                .responsable(responsable)
                .build();
    }

    public static OrdenProduccionResponseDTO toResponse(OrdenProduccion entidad) {
        OrdenProduccionResponseDTO dto = new OrdenProduccionResponseDTO();
        dto.id = entidad.getId();
        dto.codigoOrden = entidad.getCodigoOrden();
        dto.loteProduccion = entidad.getLoteProduccion();
        dto.fechaInicio = entidad.getFechaInicio();
        dto.fechaFin = entidad.getFechaFin();
        dto.cantidadProgramada = entidad.getCantidadProgramada();
        dto.cantidadProducida = entidad.getCantidadProducida();
        dto.cantidadProducidaAcumulada = entidad.getCantidadProducidaAcumulada();
        if (entidad.getCantidadProgramada() != null && entidad.getCantidadProducidaAcumulada() != null) {
            dto.cantidadRestante = BigDecimal.valueOf(entidad.getCantidadProgramada())
                    .subtract(entidad.getCantidadProducidaAcumulada());
        }
        dto.fechaUltimoCierre = entidad.getFechaUltimoCierre();
        dto.estado = entidad.getEstado().name();
        dto.nombreProducto = entidad.getProducto() != null ? entidad.getProducto().getNombre() : null;
        dto.nombreResponsable = entidad.getResponsable() != null ? entidad.getResponsable().getNombreCompleto() : null;
        return dto;
    }

    public static CierreProduccion toEntity(CierreProduccionRequestDTO dto, OrdenProduccion orden) {
        return CierreProduccion.builder()
                .cantidad(dto.getCantidad())
                .tipo(dto.getTipo())
                .cerradaIncompleta(dto.getCerradaIncompleta())
                .turno(dto.getTurno())
                .observacion(dto.getObservacion())
                .ordenProduccion(orden)
                .build();
    }

    public static CierreProduccionResponseDTO toResponse(CierreProduccion entidad) {
        CierreProduccionResponseDTO dto = new CierreProduccionResponseDTO();
        dto.id = entidad.getId();
        dto.fechaCierre = entidad.getFechaCierre();
        dto.cantidad = entidad.getCantidad();
        dto.tipo = entidad.getTipo().name();
        dto.cerradaIncompleta = entidad.getCerradaIncompleta();
        dto.turno = entidad.getTurno();
        dto.observacion = entidad.getObservacion();
        return dto;
    }

    public static EtapaProduccion toEntity(EtapaProduccionRequest dto, OrdenProduccion orden) {
        return EtapaProduccion.builder()
                .nombre(dto.nombre)
                .secuencia(dto.secuencia)
                .ordenProduccion(orden)
                .build();
    }

    public static EtapaProduccionResponse toResponse(EtapaProduccion entidad) {
        EtapaProduccionResponse dto = new EtapaProduccionResponse();
        dto.id = entidad.getId();
        dto.nombre = entidad.getNombre();
        dto.secuencia = entidad.getSecuencia();
        dto.ordenProduccionId = entidad.getOrdenProduccion() != null ? entidad.getOrdenProduccion().getId() : null;
        return dto;
    }

    public static DetalleEtapa toEntity(DetalleEtapaRequest dto, EtapaProduccion etapa, OrdenProduccion orden, Usuario operario) {
        return DetalleEtapa.builder()
                .fechaInicio(dto.fechaInicio)
                .fechaFin(dto.fechaFin)
                .observaciones(dto.observaciones)
                .etapaProduccion(etapa)
                .ordenProduccion(orden)
                .operario(operario)
                .build();
    }

    public static DetalleEtapaResponse toResponse(DetalleEtapa entidad) {
        DetalleEtapaResponse dto = new DetalleEtapaResponse();
        dto.id = entidad.getId();
        dto.fechaInicio = entidad.getFechaInicio();
        dto.fechaFin = entidad.getFechaFin();
        dto.observaciones = entidad.getObservaciones();
        dto.etapaProduccionId = entidad.getEtapaProduccion() != null ? entidad.getEtapaProduccion().getId() : null;
        dto.ordenProduccionId = entidad.getOrdenProduccion() != null ? entidad.getOrdenProduccion().getId() : null;
        dto.codigoOrden = entidad.getOrdenProduccion() != null ? entidad.getOrdenProduccion().getCodigoOrden() : null;
        dto.nombreOperario = entidad.getOperario() != null ? entidad.getOperario().getNombreCompleto() : null;
        return dto;
    }
}

