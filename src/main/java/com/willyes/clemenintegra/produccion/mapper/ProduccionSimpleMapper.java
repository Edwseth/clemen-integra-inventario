package com.willyes.clemenintegra.produccion.mapper;

import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.model.Produccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccionSimple;
import com.willyes.clemenintegra.shared.model.Usuario;

public class ProduccionSimpleMapper {

    public static Produccion toEntity(ProduccionRequest dto, Usuario usuario, Producto producto) {
        return Produccion.builder()
                .codigoLote(dto.codigoLote)
                .fechaInicio(dto.fechaInicio)
                .fechaFin(dto.fechaFin)
                .estado(EstadoProduccionSimple.valueOf(dto.estado))
                .usuario(usuario)
                .producto(producto)
                .build();
    }

    public static ProduccionResponse toResponse(Produccion entity) {
        ProduccionResponse dto = new ProduccionResponse();
        dto.id = entity.getId();
        dto.codigoLote = entity.getCodigoLote();
        dto.fechaInicio = entity.getFechaInicio();
        dto.fechaFin = entity.getFechaFin();
        dto.estado = entity.getEstado().name();
        dto.usuarioNombre = entity.getUsuario().getNombreCompleto();
        dto.productoNombre = entity.getProducto().getNombre();
        return dto;
    }
}

