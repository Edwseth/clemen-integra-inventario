package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.AjusteInventarioRequestDTO;
import com.willyes.clemenintegra.inventario.dto.AjusteInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.AjusteInventario;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T19:05:30-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class AjusteInventarioMapperImpl implements AjusteInventarioMapper {

    @Override
    public AjusteInventario toEntity(AjusteInventarioRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario) {
        if ( dto == null && producto == null && almacen == null && usuario == null ) {
            return null;
        }

        AjusteInventario ajusteInventario = new AjusteInventario();

        return ajusteInventario;
    }

    @Override
    public AjusteInventarioResponseDTO toResponseDTO(AjusteInventario entity) {
        if ( entity == null ) {
            return null;
        }

        AjusteInventarioResponseDTO ajusteInventarioResponseDTO = new AjusteInventarioResponseDTO();

        return ajusteInventarioResponseDTO;
    }
}
