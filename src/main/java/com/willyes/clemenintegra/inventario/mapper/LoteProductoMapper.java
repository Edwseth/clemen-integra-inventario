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
    @Mapping(target = "ordenProduccion", ignore = true)
    @Mapping(target = "produccion", ignore = true)
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "nombreAlmacen", source = "almacen.nombre")
    LoteProductoResponseDTO toDto(LoteProducto lote);
}
