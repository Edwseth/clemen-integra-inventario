package com.willyes.clemenintegra.bom.mapper;

import com.willyes.clemenintegra.bom.dto.*;
import com.willyes.clemenintegra.bom.model.*;
import com.willyes.clemenintegra.bom.model.enums.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;

import java.math.BigDecimal;
import java.util.stream.Collectors;

public class BomMapper {

    public static FormulaProducto toEntity(FormulaProductoRequest dto, Producto producto, Usuario usuario) {
        return FormulaProducto.builder()
                .producto(producto)
                .version(dto.version)
                .estado(EstadoFormula.valueOf(dto.estado))
                .fechaCreacion(dto.fechaCreacion)
                .creadoPor(usuario)
                .build();
    }

    public static FormulaProductoResponse toResponse(FormulaProducto entidad) {
        FormulaProductoResponse dto = new FormulaProductoResponse();
        dto.id = entidad.getId();
        dto.version = entidad.getVersion();
        dto.estado = entidad.getEstado().name();
        dto.fechaCreacion = entidad.getFechaCreacion();
        dto.productoNombre = entidad.getProducto() != null ? entidad.getProducto().getNombre() : null;
        dto.creadoPorNombre = entidad.getCreadoPor() != null ? entidad.getCreadoPor().getNombreCompleto() : null;
        if (entidad.getDetalles() != null) {
            dto.detalles = entidad.getDetalles().stream().map(BomMapper::toResponse).collect(Collectors.toList());
        }
        if (entidad.getDocumentos() != null) {
            dto.documentos = entidad.getDocumentos().stream().map(BomMapper::toResponse).collect(Collectors.toList());
        }
        return dto;
    }

    public static DetalleFormula toEntity(DetalleFormulaRequest dto, FormulaProducto formula, Producto insumo, UnidadMedida unidad) {
        return DetalleFormula.builder()
                .formula(formula)
                .insumo(insumo)
                .cantidadNecesaria(BigDecimal.valueOf(dto.cantidadNecesaria))
                .unidadMedida(unidad)
                .obligatorio(dto.obligatorio)
                .build();
    }

    public static DetalleFormulaResponse toResponse(DetalleFormula entidad) {
        DetalleFormulaResponse dto = new DetalleFormulaResponse();
        dto.id = entidad.getId();
        dto.insumoNombre = entidad.getInsumo() != null ? entidad.getInsumo().getNombre() : null;
        dto.cantidadNecesaria = entidad.getCantidadNecesaria().doubleValue();
        dto.unidad = entidad.getUnidadMedida() != null ? entidad.getUnidadMedida().getSimbolo() : null;
        dto.obligatorio = entidad.getObligatorio();
        return dto;
    }

    public static DocumentoFormula toEntity(DocumentoFormulaRequest dto, FormulaProducto formula) {
        return DocumentoFormula.builder()
                .formula(formula)
                .tipoDocumento(TipoDocumento.valueOf(dto.tipoDocumento))
                .rutaArchivo(dto.rutaArchivo)
                .build();
    }

    public static DocumentoFormulaResponse toResponse(DocumentoFormula entidad) {
        DocumentoFormulaResponse dto = new DocumentoFormulaResponse();
        dto.id = entidad.getId();
        dto.tipoDocumento = entidad.getTipoDocumento().name();
        dto.rutaArchivo = entidad.getRutaArchivo();
        return dto;
    }
}

