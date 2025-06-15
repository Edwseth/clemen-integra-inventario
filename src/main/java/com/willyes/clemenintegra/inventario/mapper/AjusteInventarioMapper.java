package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AjusteInventarioMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", source = "producto")
    @Mapping(target = "almacen", source = "almacen")
    @Mapping(target = "usuario", source = "usuario")
    AjusteInventario toEntity(AjusteInventarioRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    AjusteInventarioResponseDTO toResponseDTO(AjusteInventario entity);
}
