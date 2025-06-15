package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraRequestDTO;
import com.willyes.clemenintegra.inventario.model.OrdenCompra;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrdenCompraMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proveedor", source = "proveedor")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "fechaOrden", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "observaciones", source = "dto.observaciones")
    @Mapping(target = "detalles", ignore = true)
    OrdenCompra toEntity(OrdenCompraRequestDTO dto, Proveedor proveedor, EstadoOrdenCompra estado);
}
