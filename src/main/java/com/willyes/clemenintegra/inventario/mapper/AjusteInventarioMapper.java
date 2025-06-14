package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AjusteInventarioMapper {

    public AjusteInventario toEntity(AjusteInventarioRequestDTO dto) {
        return AjusteInventario.builder()
                .fecha(LocalDateTime.now())
                .cantidad(dto.getCantidad())
                .motivo(dto.getMotivo())
                .observaciones(dto.getObservaciones())
                .producto(Producto.builder().id(dto.getProductoId()).build())
                .almacen(Almacen.builder().id(dto.getAlmacenId()).build())
                .usuario(Usuario.builder().id(dto.getUsuarioId()).build())
                .build();
    }

    public AjusteInventarioResponseDTO toResponseDTO(AjusteInventario entity) {
        return AjusteInventarioResponseDTO.builder()
                .id(entity.getId())
                .fecha(entity.getFecha())
                .cantidad(entity.getCantidad())
                .motivo(entity.getMotivo())
                .observaciones(entity.getObservaciones())
                .productoNombre(entity.getProducto().getNombre())
                .almacenNombre(entity.getAlmacen().getNombre())
                .usuarioNombre(entity.getUsuario().getNombreCompleto())
                .build();
    }
}
