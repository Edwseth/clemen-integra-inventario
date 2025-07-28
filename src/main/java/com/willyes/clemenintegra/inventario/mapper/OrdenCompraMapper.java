package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.OrdenCompraDetalle;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrdenCompraMapper {

    default OrdenCompra toEntity(OrdenCompraRequestDTO dto, Proveedor proveedor, EstadoOrdenCompra estado) {
        OrdenCompra entity = new OrdenCompra();
        entity.setProveedor(proveedor);
        entity.setEstado(estado);
        entity.setFechaOrden(java.time.LocalDateTime.now());
        entity.setObservaciones(dto.getObservaciones());
        entity.setCodigoOrden(dto.getCodigoOrden());
        return entity;
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigoOrden", source = "codigoOrden")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "enumName")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    @Mapping(target = "fechaOrden", source = "fechaOrden")
    OrdenCompraResponseDTO toDTO(OrdenCompra orden);

    @org.mapstruct.Named("enumName")
    default String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    @Mapping(target = "proveedor", source = "proveedor")
    @Mapping(target = "detalles", source = "detalles")
    @Mapping(target = "fechaOrden", source = "fechaOrden")
    OrdenCompraConDetallesResponse toOrdenCompraConDetallesResponse(OrdenCompra orden);

    ProveedorMinResponse toProveedorMin(Proveedor proveedor);
    ProveedorResponseDTO toProveedorDTO(Proveedor proveedor);

    @Mapping(target = "producto", source = "producto", qualifiedByName = "mapProductoMini")
    OrdenCompraDetalleResponse toOrdenCompraDetalleResponse(OrdenCompraDetalle detalle);

    List<OrdenCompraDetalleResponse> toDetalleList(List<OrdenCompraDetalle> detalles);

    @Named("mapDetalleList")
    default List<OrdenCompraDetalleResponse> mapDetalleList(List<OrdenCompraDetalle> detalles) {
        return toDetalleList(detalles);
    }

    @Named("mapProductoMini")
    default ProductoMiniDTO mapProductoMini(Producto producto) {
        if (producto == null) return null;
        UnidadMiniDTO unidadDTO = new UnidadMiniDTO(
                producto.getUnidadMedida() != null ? producto.getUnidadMedida().getSimbolo() : null
        );
        return new ProductoMiniDTO(
                producto.getId().longValue(),
                producto.getNombre(),
                unidadDTO
        );
    }

}



