package com.willyes.clemenintegra.inventario.dto;

import org.springframework.core.io.Resource;

public record OrdenCompraDocumentoDescargaDTO(Resource recurso, String nombreArchivo, String contentType) {
}
