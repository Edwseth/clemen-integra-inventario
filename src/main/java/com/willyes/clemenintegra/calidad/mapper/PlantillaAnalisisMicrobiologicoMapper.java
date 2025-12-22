package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ParametroAnalisisMicrobiologicoDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicrobiologicoDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicrobiologicoResumenDTO;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlantillaAnalisisMicrobiologicoMapper {

    @Mapping(target = "productoId", source = "producto", qualifiedByName = "mapProductoId")
    @Mapping(target = "codigoSku", source = "producto", qualifiedByName = "mapProductoCodigo")
    @Mapping(target = "nombreProducto", source = "producto", qualifiedByName = "mapProductoNombre")
    @Mapping(target = "numeroVersion", source = "version")
    @Mapping(target = "creadoPorNombre", source = "creadoPor", qualifiedByName = "mapNombreUsuario")
    @Mapping(target = "fechaCreacion", source = "createdAt")
    PlantillaAnalisisMicrobiologicoResumenDTO toResumenDTO(PlantillaAnalisisMicrobiologico entity);

    @Mapping(target = "productoId", source = "producto", qualifiedByName = "mapProductoId")
    @Mapping(target = "codigoSku", source = "producto", qualifiedByName = "mapProductoCodigo")
    @Mapping(target = "nombreProducto", source = "producto", qualifiedByName = "mapProductoNombre")
    @Mapping(target = "numeroVersion", source = "version")
    @Mapping(target = "creadoPorNombre", source = "creadoPor", qualifiedByName = "mapNombreUsuario")
    @Mapping(target = "fechaCreacion", source = "createdAt")
    PlantillaAnalisisMicrobiologicoDetalleDTO toDetalleDTO(PlantillaAnalisisMicrobiologico entity);

    @Mapping(target = "nombreParametro", source = "nombreEnsayo")
    @Mapping(target = "criterioAceptacion", source = "especificacion")
    @Mapping(target = "orden", source = "orden")
    ParametroAnalisisMicrobiologicoDTO toParametroDTO(ParametroAnalisisMicrobiologico parametro);

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
