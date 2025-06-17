package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.DetalleFormulaRequest;
import com.willyes.clemenintegra.bom.dto.DetalleFormulaResponse;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaRequestDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.dto.FormulaProductoRequest;
import com.willyes.clemenintegra.bom.dto.FormulaProductoResponse;
import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.DocumentoFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.shared.model.Usuario;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-16T19:05:30-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class BomMapperImpl implements BomMapper {

    @Override
    public FormulaProducto toEntity(FormulaProductoRequest dto, Producto producto, Usuario usuario) {
        if ( dto == null && producto == null && usuario == null ) {
            return null;
        }

        FormulaProducto formulaProducto = new FormulaProducto();

        return formulaProducto;
    }

    @Override
    public FormulaProductoResponse toResponse(FormulaProducto entidad) {
        if ( entidad == null ) {
            return null;
        }

        FormulaProductoResponse formulaProductoResponse = new FormulaProductoResponse();

        return formulaProductoResponse;
    }

    @Override
    public DetalleFormula toEntity(DetalleFormulaRequest dto, FormulaProducto formula, Producto insumo, UnidadMedida unidad) {
        if ( dto == null && formula == null && insumo == null && unidad == null ) {
            return null;
        }

        DetalleFormula detalleFormula = new DetalleFormula();

        return detalleFormula;
    }

    @Override
    public DetalleFormulaResponse toResponse(DetalleFormula entidad) {
        if ( entidad == null ) {
            return null;
        }

        DetalleFormulaResponse detalleFormulaResponse = new DetalleFormulaResponse();

        return detalleFormulaResponse;
    }

    @Override
    public DocumentoFormula toEntity(DocumentoFormulaRequestDTO dto, FormulaProducto formula) {
        if ( dto == null && formula == null ) {
            return null;
        }

        DocumentoFormula documentoFormula = new DocumentoFormula();

        return documentoFormula;
    }

    @Override
    public DocumentoFormulaResponseDTO toResponse(DocumentoFormula entidad) {
        if ( entidad == null ) {
            return null;
        }

        DocumentoFormulaResponseDTO documentoFormulaResponseDTO = new DocumentoFormulaResponseDTO();

        return documentoFormulaResponseDTO;
    }
}
