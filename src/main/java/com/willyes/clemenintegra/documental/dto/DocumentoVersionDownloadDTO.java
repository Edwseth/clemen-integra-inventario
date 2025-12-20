package com.willyes.clemenintegra.documental.dto;

import org.springframework.core.io.Resource;

public record DocumentoVersionDownloadDTO(Resource recurso, String nombreArchivo, String contentType) {
}
