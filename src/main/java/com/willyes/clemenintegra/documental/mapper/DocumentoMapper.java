package com.willyes.clemenintegra.documental.mapper;

import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.model.Documento;
import com.willyes.clemenintegra.documental.model.DocumentoVersion;
import com.willyes.clemenintegra.shared.model.Usuario;

public final class DocumentoMapper {

    private DocumentoMapper() {
    }

    public static DocumentoDTO toDTO(Documento documento, DocumentoVersion versionVigente) {
        if (documento == null) {
            return null;
        }
        return DocumentoDTO.builder()
                .id(documento.getId())
                .codigo(documento.getCodigo())
                .nombre(documento.getNombre())
                .tipo(documento.getTipo())
                .area(documento.getArea())
                .estado(documento.getEstado())
                .descripcion(documento.getDescripcion())
                .fechaCreacion(documento.getFechaCreacion())
                .creadoPorNombre(nombreUsuario(documento.getCreadoPor()))
                .numeroVersionVigente(versionVigente != null ? versionVigente.getNumeroVersion() : null)
                .fechaEmisionVersionVigente(versionVigente != null ? versionVigente.getFechaEmision() : null)
                .versionVigenteId(versionVigente != null ? versionVigente.getId() : null)
                .build();
    }

    public static DocumentoVersionDTO toVersionDTO(DocumentoVersion version) {
        if (version == null) {
            return null;
        }
        return DocumentoVersionDTO.builder()
                .id(version.getId())
                .numeroVersion(version.getNumeroVersion())
                .fechaEmision(version.getFechaEmision())
                .nombreVisible(version.getNombreVisible())
                .nombreArchivoOriginal(version.getNombreArchivoOriginal())
                .vigente(version.isVigente())
                .emitidoPorNombre(nombreUsuario(version.getEmitidoPor()))
                .comentarios(version.getComentarios())
                .tamanoBytes(version.getTamanoBytes())
                .contentType(version.getContentType())
                .build();
    }

    private static String nombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombreCompleto = usuario.getNombreCompleto();
        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            return nombreCompleto;
        }
        return usuario.getNombreUsuario();
    }
}
