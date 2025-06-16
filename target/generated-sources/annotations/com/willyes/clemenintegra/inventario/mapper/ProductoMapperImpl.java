package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T18:54:38-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ProductoMapperImpl implements ProductoMapper {

    @Override
    public ProductoResponseDTO toDto(Producto producto) {
        if ( producto == null ) {
            return null;
        }

        Producto producto1 = null;

        producto1 = producto;

        ProductoResponseDTO productoResponseDTO = new ProductoResponseDTO( producto1 );

        return productoResponseDTO;
    }
}
