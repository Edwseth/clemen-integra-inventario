package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDetalleResponse;
import com.willyes.clemenintegra.inventario.dto.ProductoMiniDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMiniDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrdenCompraDetalleMapper {

    @Mapping(target = "producto", source = "producto", qualifiedByName = "mapProductoMini")
    @Mapping(target = "fechaNecesidad", source = "fechaNecesidad")
    OrdenCompraDetalleResponse toResponse(OrdenCompraDetalle entity);

    @Named("mapProductoMini")
    default ProductoMiniDTO mapProductoMini(com.willyes.clemenintegra.inventario.model.Producto producto) {
        if (producto == null) return null;

        String udmSimbolo = null;
        if (producto.getUnidadMedida() != null) {
            String imp = producto.getUnidadMedida().getSimboloImpresion();
            String sim = producto.getUnidadMedida().getSimbolo();
            udmSimbolo = (imp != null && !imp.isBlank()) ? imp : sim;
        }

        UnidadMiniDTO unidadDTO = new UnidadMiniDTO(udmSimbolo);
        return new ProductoMiniDTO(
                producto.getId().longValue(),
                producto.getNombre(),
                unidadDTO
        );
    }

}



