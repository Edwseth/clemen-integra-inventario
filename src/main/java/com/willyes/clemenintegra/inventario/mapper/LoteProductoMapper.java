package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoteProductoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", source = "producto")
    @Mapping(target = "almacen", source = "almacen")
    @Mapping(target = "usuarioLiberador", source = "usuario")
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    LoteProductoResponseDTO toDto(LoteProducto lote);
}
