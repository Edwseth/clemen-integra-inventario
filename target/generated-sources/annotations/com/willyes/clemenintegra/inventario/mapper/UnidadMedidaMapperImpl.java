package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-15T12:57:22-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UnidadMedidaMapperImpl implements UnidadMedidaMapper {

    @Override
    public UnidadMedidaResponseDTO toDto(UnidadMedida unidad) {
        if ( unidad == null ) {
            return null;
        }

        UnidadMedidaResponseDTO unidadMedidaResponseDTO = new UnidadMedidaResponseDTO();

        return unidadMedidaResponseDTO;
    }
}
