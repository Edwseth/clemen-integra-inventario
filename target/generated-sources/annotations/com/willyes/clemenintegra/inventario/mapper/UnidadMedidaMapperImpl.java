package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T19:27:31-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class UnidadMedidaMapperImpl implements UnidadMedidaMapper {

    @Override
    public UnidadMedidaResponseDTO toDto(UnidadMedida unidad) {
        if ( unidad == null ) {
            return null;
        }

        UnidadMedidaResponseDTO.UnidadMedidaResponseDTOBuilder unidadMedidaResponseDTO = UnidadMedidaResponseDTO.builder();

        unidadMedidaResponseDTO.id( unidad.getId() );
        unidadMedidaResponseDTO.nombre( unidad.getNombre() );
        unidadMedidaResponseDTO.simbolo( unidad.getSimbolo() );

        return unidadMedidaResponseDTO.build();
    }
}
