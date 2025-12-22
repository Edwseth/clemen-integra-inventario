package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.EspecificacionFisicoQuimicaDTO;
import com.willyes.clemenintegra.calidad.dto.EspecificacionFisicoQuimicaRequest;
import com.willyes.clemenintegra.calidad.model.EspecificacionFisicoQuimica;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EspecificacionFisicoQuimicaMapper {

    @Mapping(target = "productoId", source = "producto", qualifiedByName = "mapProductoId")
    @Mapping(target = "codigoSku", source = "producto", qualifiedByName = "mapProductoCodigo")
    @Mapping(target = "nombreProducto", source = "producto", qualifiedByName = "mapProductoNombre")
    @Mapping(target = "creadoPorNombre", source = "creadoPor", qualifiedByName = "mapNombreUsuario")
    @Mapping(target = "fechaCreacion", source = "createdAt")
    EspecificacionFisicoQuimicaDTO toDTO(EspecificacionFisicoQuimica entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", source = "producto")
    @Mapping(target = "creadoPor", source = "creadoPor")
    @Mapping(target = "actualizadoPor", ignore = true)
    @Mapping(target = "activo", expression = "java(dto.getActivo() == null || dto.getActivo())")
    EspecificacionFisicoQuimica toEntity(EspecificacionFisicoQuimicaRequest dto, Producto producto, Usuario creadoPor);

    @Named("mapProductoId")
    default Long mapProductoId(Producto producto) {
        return (producto != null && producto.getId() != null) ? producto.getId().longValue() : null;
    }

    @Named("mapProductoCodigo")
    default String mapProductoCodigo(Producto producto) {
        return (producto != null) ? producto.getCodigoSku() : null;
    }

    @Named("mapProductoNombre")
    default String mapProductoNombre(Producto producto) {
        return (producto != null) ? producto.getNombre() : null;
    }

    @Named("mapNombreUsuario")
    default String mapNombreUsuario(Usuario usuario) {
        return (usuario != null) ? usuario.getNombreCompleto() : null;
    }
}
