package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoteProductoMapper {

    @Mapping(target = "producto", source = "producto")
    @Mapping(target = "almacen", source = "almacen")
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen);

    @Mapping(target = "nombreProducto", source = "producto.nombre")
    @Mapping(target = "nombreAlmacen", source = "almacen.nombre")
    LoteProductoResponseDTO toDto(LoteProducto lote);
}
