package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.mapstruct.*;
import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    default ProductoResponseDTO safeToDto(Producto producto) {
        if (producto == null) {
            System.out.println("❌ Producto nulo detectado");
            return null;
        }
        return toDto(producto);
    }

    @Mapping(source = "codigoSku", target = "sku")
    @Mapping(target = "unidadMedida", source = "unidadMedida")
    @Mapping(target = "categoria", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null)")
    @Mapping(target = "tipoAnalisisCalidad", expression = "java(producto.getTipoAnalisis() != null ? producto.getTipoAnalisis().name() : null)")
    @Mapping(target = "rendimiento", expression = "java(producto.getRendimientoUnidad() != null ? producto.getRendimientoUnidad() : BigDecimal.ZERO)")
    // PROD-DETAIL-IDS BEGIN
    @Mapping(target = "unidadMedidaId", expression = "java(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getId() : null)")
    @Mapping(target = "categoriaProductoId", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getId() : null)")
        // PROD-DETAIL-IDS END
    ProductoResponseDTO toDto(Producto producto);

    UnidadMedidaResponseDTO toUnidadMedidaDto(UnidadMedida unidadMedida);

    @Named("mapCategoriaProducto")
    default String mapCategoriaProducto(CategoriaProducto categoriaProducto) {
        return (categoriaProducto != null) ? categoriaProducto.getNombre() : null;
    }

    @Named("mapTipoAnalisisCalidad")
    default TipoAnalisisCalidad mapTipoAnalisisCalidad(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoAnalisisCalidad.NINGUNO;
        }
        return TipoAnalisisCalidad.valueOf(valor);
    }

    @Named("mapTipoAnalisisCalidadString")
    default String mapTipoAnalisisCalidadString(TipoAnalisisCalidad valor) {
        return valor != null ? valor.name() : null;
    }

}


