package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", source = "dto.fechaCreacion")
    @Mapping(target = "producto", source = "producto")
    @Mapping(target = "estado", expression = "java(EstadoFormula.valueOf(dto.estado))")
    @Mapping(target = "creadoPor", source = "usuario")
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "documentos", ignore = true)
    FormulaProducto toEntity(FormulaProductoRequest dto, Producto producto, Usuario usuario);

    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "estado", expression = "java(entidad.getEstado().name())")
    @Mapping(target = "creadoPorNombre", source = "creadoPor.nombreCompleto")
    FormulaProductoResponse toResponse(FormulaProducto entidad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formula", source = "formula")
    @Mapping(target = "insumo", source = "insumo")
    @Mapping(target = "unidadMedida", source = "unidad")
    @Mapping(target = "cantidadNecesaria", expression = "java(java.math.BigDecimal.valueOf(dto.cantidadNecesaria))")
    DetalleFormula toEntity(DetalleFormulaRequest dto, FormulaProducto formula, Producto insumo, UnidadMedida unidad);

    @Mapping(target = "insumoNombre", source = "insumo.nombre")
    @Mapping(target = "cantidadNecesaria", expression = "java(entidad.getCantidadNecesaria().doubleValue())")
    @Mapping(target = "unidad", source = "unidadMedida.nombre")
    DetalleFormulaResponse toResponse(DetalleFormula entidad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "formula", source = "formula")
    @Mapping(target = "tipoDocumento", expression = "java(TipoDocumento.valueOf(dto.getTipoDocumento()))")
    DocumentoFormula toEntity(DocumentoFormulaRequestDTO dto, FormulaProducto formula);

    @Mapping(target = "tipoDocumento", expression = "java(entidad.getTipoDocumento().name())")
    DocumentoFormulaResponseDTO toResponse(DocumentoFormula entidad);
}
