package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AjusteInventarioMapper {

    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "fecha", ignore = true)
    //@Mapping(target = "producto", source = "producto")
    //@Mapping(target = "almacen", source = "almacen")
    //@Mapping(target = "usuario", source = "usuario")
    AjusteInventario toEntity(AjusteInventarioRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    //@Mapping(target = "productoNombre", expression = "java(entity.getProducto() != null ? entity.getProducto().getNombre() : null)")
    //@Mapping(target = "almacenNombre", expression = "java(entity.getAlmacen() != null ? entity.getAlmacen().getNombre() : null)")
    //@Mapping(target = "usuarioNombre", expression = "java(entity.getUsuario() != null ? entity.getUsuario().getNombreCompleto() : null)")
    @Mapping(target = "productoNombre", expression = "java(mapProductoNombre(entity.getProducto()))")
    @Mapping(target = "almacenNombre", expression = "java(mapAlmacenNombre(entity.getAlmacen()))")
    @Mapping(target = "usuarioNombre", expression = "java(mapUsuarioNombre(entity.getUsuario()))")
    AjusteInventarioResponseDTO toResponseDTO(AjusteInventario entity);

    @Named("mapProductoNombre")
    default String mapProductoNombre(Producto producto) {
        return (producto != null) ? producto.getNombre() : null;
    }

    @Named("mapAlmacenNombre")
    default String mapAlmacenNombre(Almacen almacen) {
        return (almacen != null) ? almacen.getNombre() : null;
    }

    @Named("mapUsuarioNombre")
    default String mapUsuarioNombre(Usuario usuario) {
        return (usuario != null) ? usuario.getNombreCompleto() : null;
    }
}


