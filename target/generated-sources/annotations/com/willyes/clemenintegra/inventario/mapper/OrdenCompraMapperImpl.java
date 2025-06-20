package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T12:15:58-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class OrdenCompraMapperImpl implements OrdenCompraMapper {

    @Override
    public OrdenCompraResponseDTO toDTO(OrdenCompra orden) {
        if ( orden == null ) {
            return null;
        }

        OrdenCompraResponseDTO ordenCompraResponseDTO = new OrdenCompraResponseDTO();

        return ordenCompraResponseDTO;
    }
}
