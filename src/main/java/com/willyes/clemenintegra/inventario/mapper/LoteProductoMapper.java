package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoteProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaFabricacion", expression = "java(dto.getFechaFabricacion())")
    @Mapping(target = "fechaVencimiento", expression = "java(dto.getFechaVencimiento())")
    @Mapping(target = "fechaLiberacion", expression = "java(dto.getFechaLiberacion())")
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    // Mapeo de respuesta
    @Mapping(target = "nombreAlmacen", expression = "java(lote.getAlmacen()!=null ? lote.getAlmacen().getNombre() : null)")
    @Mapping(target = "ubicacionAlmacen", expression = "java(lote.getAlmacen()!=null ? lote.getAlmacen().getUbicacion() : null)")
    @Mapping(target = "nombreProducto", expression = "java(lote.getProducto()!=null ? lote.getProducto().getNombre() : null)")
    @Mapping(target = "nombreUsuarioLiberador", expression = "java(lote.getUsuarioLiberador()!=null ? lote.getUsuarioLiberador().getNombreCompleto() : null)")
    @Mapping(target = "evaluaciones", ignore = true)
    LoteProductoResponseDTO toResponseDTO(LoteProducto lote);

    @Mapping(source = "producto.nombre", target = "nombreProducto")
    @Mapping(source = "almacen.nombre", target = "nombreAlmacen")
    @Mapping(source = "almacen.ubicacion", target = "ubicacionAlmacen")
    @Mapping(source = "usuarioLiberador.nombreCompleto", target = "nombreUsuarioLiberador")
    @Mapping(target = "evaluaciones", ignore = true)
    LoteProductoResponseDTO toDto(LoteProducto entity);

}



