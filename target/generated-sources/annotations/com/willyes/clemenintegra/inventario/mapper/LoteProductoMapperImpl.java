package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T18:54:38-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LoteProductoMapperImpl implements LoteProductoMapper {

    @Override
    public LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario) {
        if ( dto == null && producto == null && almacen == null && usuario == null ) {
            return null;
        }

        Long id = null;

        LoteProducto loteProducto = new LoteProducto( id );

        return loteProducto;
    }

    @Override
    public LoteProductoResponseDTO toDto(LoteProducto lote) {
        if ( lote == null ) {
            return null;
        }

        LoteProductoResponseDTO loteProductoResponseDTO = new LoteProductoResponseDTO();

        return loteProductoResponseDTO;
    }
}
