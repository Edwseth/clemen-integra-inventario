package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T18:54:38-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ProveedorMapperImpl implements ProveedorMapper {

    @Override
    public Proveedor toEntity(ProveedorRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Long id = null;

        Proveedor proveedor = new Proveedor( id );

        return proveedor;
    }
}
