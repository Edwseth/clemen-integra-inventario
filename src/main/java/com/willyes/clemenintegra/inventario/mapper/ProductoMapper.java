package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "unidadMedida", expression = "java(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : null)")
    @Mapping(target = "categoria", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null)")
    ProductoResponseDTO toDto(Producto producto);

    @Named("mapUnidadMedida")
    default String mapUnidadMedida(UnidadMedida unidadMedida) {
        return (unidadMedida != null) ? unidadMedida.getNombre() : null;
    }

    @Named("mapCategoriaProducto")
    default String mapCategoriaProducto(CategoriaProducto categoriaProducto) {
        return (categoriaProducto != null) ? categoriaProducto.getNombre() : null;
    }
}


