package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraRequestDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraResponseDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrdenCompraMapper {

    default OrdenCompra toEntity(OrdenCompraRequestDTO dto, Proveedor proveedor, EstadoOrdenCompra estado) {
        OrdenCompra entity = new OrdenCompra();
        entity.setProveedor(proveedor);
        entity.setEstado(estado);
        entity.setFechaOrden(java.time.LocalDate.now());
        entity.setObservaciones(dto.getObservaciones());
        entity.setCodigoOrden(dto.getCodigoOrden);
        // los detalles los dejamos null o vacíos según corresponda
        return entity;
    }

    OrdenCompraResponseDTO toDTO(OrdenCompra orden);
}



