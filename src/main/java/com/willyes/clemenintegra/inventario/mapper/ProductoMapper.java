package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    default ProductoResponseDTO safeToDto(Producto producto) {
        if (producto == null) {
            System.out.println("❌ Producto nulo detectado");
            return null;
        }
        return toDto(producto);
    }

    @Mapping(target = "unidadMedida", expression = "java(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getNombre() : null)")
    @Mapping(target = "categoria", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null)")
    @Mapping(target = "tipoAnalisis", expression = "java(producto.getRequiereInspeccion() ? TipoAnalisisCalidad.INSPECCION : TipoAnalisisCalidad.NINGUNO)")
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


