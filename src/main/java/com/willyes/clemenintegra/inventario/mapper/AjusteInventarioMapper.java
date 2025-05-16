package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import org.springframework.stereotype.Component;

@Component
public class AjusteInventarioMapper {

    public AjusteInventarioDTO toDTO(AjusteInventario entity) {
        return AjusteInventarioDTO.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .cantidad(entity.getCantidad())
                .motivo(entity.getMotivo())
                .observaciones(entity.getObservaciones())
                .productoId(entity.getProducto().getId())
                .almacenId(entity.getAlmacen().getId())
                .usuarioId(entity.getUsuario().getId())
                .build();
    }

    public AjusteInventario toEntity(AjusteInventarioDTO dto) {
        return AjusteInventario.builder()
                .id(dto.getId())
                .fecha(dto.getFecha())
                .cantidad(dto.getCantidad())
                .motivo(dto.getMotivo())
                .observaciones(dto.getObservaciones())
                .producto(Producto.builder().id(dto.getProductoId()).build())
                .almacen(Almacen.builder().id(dto.getAlmacenId()).build())
                .usuario(Usuario.builder().id(dto.getUsuarioId()).build())
                .build();
    }
}

