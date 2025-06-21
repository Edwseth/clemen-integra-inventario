package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrdenCompraMapper {

    default OrdenCompra toEntity(OrdenCompraRequestDTO dto, Proveedor proveedor, EstadoOrdenCompra estado) {
        OrdenCompra entity = new OrdenCompra();
        entity.setProveedor(proveedor);
        entity.setEstado(estado);
        entity.setFechaOrden(java.time.LocalDate.now());
        entity.setObservaciones(dto.getObservaciones());
        entity.setCodigoOrden(dto.getCodigoOrden());
        // los detalles los dejamos null o vacíos según corresponda
        return entity;
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigoOrden", source = "codigoOrden")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "enumName")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombre")
    OrdenCompraResponseDTO toDTO(OrdenCompra orden);

    @org.mapstruct.Named("enumName")
    default String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}



