package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-15T12:57:21-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class CategoriaProductoMapperImpl implements CategoriaProductoMapper {

    @Override
    public CategoriaProductoResponseDTO toDto(CategoriaProducto categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaProductoResponseDTO categoriaProductoResponseDTO = new CategoriaProductoResponseDTO();

        return categoriaProductoResponseDTO;
    }
}
