package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;

public class HistorialEstadoOrdenMapper {

    public static HistorialEstadoOrden toEntity(HistorialEstadoOrdenRequest dto, OrdenCompra orden, Usuario usuario) {
        return HistorialEstadoOrden.builder()
                .ordenCompra(orden)
                .estado(EstadoOrdenCompra.valueOf(dto.estado))
                .fechaCambio(dto.fechaCambio)
                .cambiadoPor(usuario)
                .observaciones(dto.observaciones)
                .build();
    }

    public static HistorialEstadoOrdenResponse toResponse(HistorialEstadoOrden entity) {
        HistorialEstadoOrdenResponse dto = new HistorialEstadoOrdenResponse();
        dto.id = entity.getId();
        dto.estado = entity.getEstado().name();
        dto.fechaCambio = entity.getFechaCambio();
        dto.nombreUsuario = entity.getCambiadoPor().getNombreCompleto();
        dto.observaciones = entity.getObservaciones();
        return dto;
    }
}

