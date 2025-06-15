package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    FormulaProducto toEntity(FormulaProductoRequest dto, Producto producto, Usuario usuario);

    FormulaProductoResponse toResponse(FormulaProducto entidad);

    @Mapping(target = "id", ignore = true)
    DetalleFormula toEntity(DetalleFormulaRequest dto, FormulaProducto formula, Producto insumo, UnidadMedida unidad);

    DetalleFormulaResponse toResponse(DetalleFormula entidad);

    @Mapping(target = "id", ignore = true)
    DocumentoFormula toEntity(DocumentoFormulaRequestDTO dto, FormulaProducto formula);

    DocumentoFormulaResponseDTO toResponse(DocumentoFormula entidad);
}


