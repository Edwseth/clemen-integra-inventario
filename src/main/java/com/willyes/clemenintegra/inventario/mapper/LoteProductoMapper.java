package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LoteProductoMapper {

    @Mapping(target = "id", ignore = true)
    //@Mapping(source = "producto", target = "producto")
    //@Mapping(source = "almacen", target = "almacen")
    //@Mapping(source = "usuario", target = "usuarioLiberador")
    //@Mapping(target = "ordenProduccion", ignore = true)
    //@Mapping(target = "produccion", ignore = true)
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    // Mapeo de respuesta
    @Mapping(target = "nombreProducto", expression = "java(mapNombreProducto(lote.getProducto()))")
    @Mapping(target = "nombreAlmacen", expression = "java(mapNombreAlmacen(lote.getAlmacen()))")
    @Mapping(target = "nombreUsuarioLiberador", expression = "java(mapNombreUsuario(lote.getUsuarioLiberador()))")
    LoteProductoResponseDTO toResponseDTO(LoteProducto lote);

    @Named("mapNombreProducto")
    default String mapNombreProducto(Producto producto) {
        return (producto != null) ? producto.getNombre() : null;
    }

    @Named("mapNombreAlmacen")
    default String mapNombreAlmacen(Almacen almacen) {
        return (almacen != null) ? almacen.getNombre() : null;
    }

    @Named("mapNombreUsuario")
    default String mapNombreUsuario(Usuario usuario) {return (usuario != null) ? usuario.getNombreCompleto() : null;}
}



