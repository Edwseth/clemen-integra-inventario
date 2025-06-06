package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoResponseDTO toDto(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setCodigoSku(producto.getCodigoSku());
        dto.setNombre(producto.getNombre());
        dto.setDescripcionProducto(producto.getDescripcionProducto());
        dto.setFechaCreacion(producto.getFechaCreacion());
        dto.setStockActual(producto.getStockActual());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setStockMinimoProveedor(producto.getStockMinimoProveedor());
        dto.setRequiereInspeccion(producto.isRequiereInspeccion());
        dto.setActivo(producto.isActivo());

        if (producto.getUnidadMedida() != null) {
            dto.setUnidadMedida(producto.getUnidadMedida().getNombre());
        }
        if (producto.getCategoriaProducto() != null) {
            dto.setCategoria(producto.getCategoriaProducto().getNombre());
        }

        return dto;
    }
}

