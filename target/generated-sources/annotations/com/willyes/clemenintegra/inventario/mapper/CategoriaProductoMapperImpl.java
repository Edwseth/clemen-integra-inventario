package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.CategoriaProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T19:27:31-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CategoriaProductoMapperImpl implements CategoriaProductoMapper {

    @Override
    public CategoriaProductoResponseDTO toDto(CategoriaProducto categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaProductoResponseDTO.CategoriaProductoResponseDTOBuilder categoriaProductoResponseDTO = CategoriaProductoResponseDTO.builder();

        categoriaProductoResponseDTO.id( categoria.getId() );
        categoriaProductoResponseDTO.nombre( categoria.getNombre() );
        categoriaProductoResponseDTO.tipo( categoria.getTipo() );

        return categoriaProductoResponseDTO.build();
    }
}
