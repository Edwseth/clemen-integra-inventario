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
    date = "2025-06-20T19:27:31-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LoteProductoMapperImpl implements LoteProductoMapper {

    @Override
    public LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario) {
        if ( dto == null && producto == null && almacen == null && usuario == null ) {
            return null;
        }

        LoteProducto.LoteProductoBuilder loteProducto = LoteProducto.builder();

        if ( dto != null ) {
            loteProducto.codigoLote( dto.getCodigoLote() );
            loteProducto.fechaFabricacion( dto.getFechaFabricacion() );
            loteProducto.fechaVencimiento( dto.getFechaVencimiento() );
            loteProducto.stockLote( dto.getStockLote() );
            loteProducto.estado( dto.getEstado() );
            loteProducto.temperaturaAlmacenamiento( dto.getTemperaturaAlmacenamiento() );
            loteProducto.fechaLiberacion( dto.getFechaLiberacion() );
        }
        loteProducto.producto( producto );
        loteProducto.almacen( almacen );

        return loteProducto.build();
    }

    @Override
    public LoteProductoResponseDTO toDto(LoteProducto lote) {
        if ( lote == null ) {
            return null;
        }

        LoteProductoResponseDTO.LoteProductoResponseDTOBuilder loteProductoResponseDTO = LoteProductoResponseDTO.builder();

        loteProductoResponseDTO.id( lote.getId() );
        loteProductoResponseDTO.codigoLote( lote.getCodigoLote() );
        loteProductoResponseDTO.fechaFabricacion( lote.getFechaFabricacion() );
        loteProductoResponseDTO.fechaVencimiento( lote.getFechaVencimiento() );
        loteProductoResponseDTO.stockLote( lote.getStockLote() );
        loteProductoResponseDTO.estado( lote.getEstado() );
        loteProductoResponseDTO.temperaturaAlmacenamiento( lote.getTemperaturaAlmacenamiento() );
        loteProductoResponseDTO.fechaLiberacion( lote.getFechaLiberacion() );

        return loteProductoResponseDTO.build();
    }
}
