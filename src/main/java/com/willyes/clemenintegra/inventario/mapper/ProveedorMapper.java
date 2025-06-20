package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    Proveedor toEntity(ProveedorRequestDTO dto);
    ProveedorResponseDTO toDTO(Proveedor entity);

}
