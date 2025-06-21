package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T19:27:31-0500",
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

        ordenCompraResponseDTO.setEstado( enumName( orden.getEstado() ) );
        ordenCompraResponseDTO.setProveedorNombre( ordenProveedorNombre( orden ) );
        ordenCompraResponseDTO.setId( orden.getId() );
        ordenCompraResponseDTO.setCodigoOrden( orden.getCodigoOrden() );

        return ordenCompraResponseDTO;
    }

    private String ordenProveedorNombre(OrdenCompra ordenCompra) {
        if ( ordenCompra == null ) {
            return null;
        }
        Proveedor proveedor = ordenCompra.getProveedor();
        if ( proveedor == null ) {
            return null;
        }
        String nombre = proveedor.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
