package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "dto.fechaCreacion", target = "fechaCreacion")
    @Mapping(target = "activo", ignore = true)
    //@Mapping(source = "producto", target = "producto")
    @Mapping(source = "usuario", target = "creadoPor")
    //@Mapping(target = "estado", expression = "java(EstadoFormula.valueOf(dto.estado))")
    //@Mapping(target = "detalles", ignore = true)
    //@Mapping(target = "documentos", ignore = true)
    FormulaProducto toEntity(FormulaProductoRequest dto, Producto producto, Usuario usuario);

    @Mapping(target = "productoNombre", source = "producto", qualifiedByName = "mapProductoNombre")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "mapEstadoFormula")
    @Mapping(target = "creadoPorNombre", source = "creadoPor", qualifiedByName = "mapNombreUsuario")
    @Mapping(target = "actualizadoPorNombre", source = "actualizadoPor", qualifiedByName = "mapNombreUsuario")
    FormulaProductoResponse toResponseDTO(FormulaProducto formula);

    @Mapping(target = "id", ignore = true)
    //@Mapping(target = "formula", source = "formula")
    //@Mapping(target = "insumo", source = "insumo")
    //@Mapping(target = "unidadMedida", source = "unidad")
    //@Mapping(target = "cantidadNecesaria", expression = "java(java.math.BigDecimal.valueOf(dto.cantidadNecesaria))")
    DetalleFormula toEntity(DetalleFormulaRequest dto, FormulaProducto formula, Producto insumo, UnidadMedida unidad);

    @Mapping(target = "insumoNombre", source = "insumo", qualifiedByName = "mapProductoNombre")
    @Mapping(target = "unidad", source = "unidadMedida", qualifiedByName = "mapUnidadNombre")
    @Mapping(target = "unidadSimbolo", source = "unidadMedida", qualifiedByName = "mapUnidadSimbolo")
    DetalleFormulaResponse toResponseDTO(DetalleFormula detalle);

    //@Mapping(target = "id", ignore = true)
    //@Mapping(target = "formula", source = "formula")
    //@Mapping(target = "tipoDocumento", expression = "java(TipoDocumento.valueOf(dto.getTipoDocumento()))")
    DocumentoFormula toEntity(DocumentoFormulaRequestDTO dto, FormulaProducto formula);

    @Mapping(target = "tipoDocumento", source = "tipoDocumento", qualifiedByName = "mapTipoDocumento")
    DocumentoFormulaResponseDTO toResponseDTO(DocumentoFormula documento);

    @Named("mapProductoNombre")
    default String mapProductoNombre(Producto producto) {
        return (producto != null) ? producto.getNombre() : null;
    }

    @Named("mapUnidadNombre")
    default String mapUnidadNombre(UnidadMedida unidad) {
        return (unidad != null) ? unidad.getNombre() : null;
    }

    @Named("mapUnidadSimbolo")
    default String mapUnidadSimbolo(UnidadMedida unidad) {
        return (unidad != null) ? unidad.getSimbolo() : null;
    }

    @Named("mapNombreUsuario")
    default String mapNombreUsuario(Usuario usuario) {
        return (usuario != null) ? usuario.getNombreCompleto() : null;
    }

    @Named("mapEstadoFormula")
    default String mapEstadoFormula(EstadoFormula estado) {
        return (estado != null) ? estado.name() : null;
    }

    @Named("mapTipoDocumento")
    default String mapTipoDocumento(TipoDocumento tipoDocumento) {
        return (tipoDocumento != null) ? tipoDocumento.name() : null;
    }
}
