package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.model.Proveedor;

public class ProveedorMapper {

    public static Proveedor toEntity(ProveedorRequestDTO dto) {
        return Proveedor.builder()
                .nombre(dto.getNombre())
                .identificacion(dto.getIdentificacion())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .direccion(dto.getDireccion())
                .paginaWeb(dto.getPaginaWeb())
                .nombreContacto(dto.getNombreContacto())
                .activo(dto.getActivo())
                .build();
    }
}

