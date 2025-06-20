package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProveedorRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T12:39:13-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ProveedorMapperImpl implements ProveedorMapper {

    @Override
    public Proveedor toEntity(ProveedorRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Proveedor.ProveedorBuilder proveedor = Proveedor.builder();

        proveedor.nombre( dto.getNombre() );
        proveedor.identificacion( dto.getIdentificacion() );
        proveedor.telefono( dto.getTelefono() );
        proveedor.email( dto.getEmail() );
        proveedor.direccion( dto.getDireccion() );
        proveedor.paginaWeb( dto.getPaginaWeb() );
        proveedor.nombreContacto( dto.getNombreContacto() );
        proveedor.activo( dto.getActivo() );

        return proveedor.build();
    }

    @Override
    public ProveedorResponseDTO toDTO(Proveedor entity) {
        if ( entity == null ) {
            return null;
        }

        ProveedorResponseDTO proveedorResponseDTO = new ProveedorResponseDTO();

        proveedorResponseDTO.setId( entity.getId() );
        proveedorResponseDTO.setNombre( entity.getNombre() );

        return proveedorResponseDTO;
    }
}
