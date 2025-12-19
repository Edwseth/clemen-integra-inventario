package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDTO;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidad;
import com.willyes.clemenintegra.calidad.model.DocumentoCalidadVersion;

public final class DocumentoCalidadMapper {

    private DocumentoCalidadMapper() {
    }

    public static DocumentoCalidadDTO toDTO(DocumentoCalidad entity) {
        if (entity == null) {
            return null;
        }
        return DocumentoCalidadDTO.builder()
                .id(entity.getId())
                .tipo(entity.getTipo())
                .codigo(entity.getCodigo())
                .nombre(entity.getNombre())
                .estado(entity.getEstado())
                .loteId(entity.getLote() != null ? entity.getLote().getId() : null)
                .creadoPorId(entity.getCreadoPorId())
                .fechaCreacion(entity.getFechaCreacion())
                .actualizadoPorId(entity.getActualizadoPorId())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }

    public static DocumentoCalidadVersionDTO toVersionDTO(DocumentoCalidadVersion entity) {
        if (entity == null) {
            return null;
        }
        return DocumentoCalidadVersionDTO.builder()
                .id(entity.getId())
                .documentoId(entity.getDocumento() != null ? entity.getDocumento().getId() : null)
                .version(entity.getVersion())
                .nombreArchivo(entity.getNombreArchivo())
                .nombreVisible(entity.getNombreVisible())
                .contentType(entity.getContentType())
                .sizeBytes(entity.getSizeBytes())
                .storagePath(entity.getStoragePath())
                .hashOpcional(entity.getHashOpcional())
                .creadoPorId(entity.getCreadoPorId())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
