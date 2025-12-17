package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.DocumentoMetaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoDescargaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrdenCompraDocumentoService {

    List<OrdenCompraDocumentoResponseDTO> subirDocumentos(Long ordenCompraId,
                                                          List<MultipartFile> archivos,
                                                          List<DocumentoMetaDTO> metadata,
                                                          Long usuarioId);

    List<OrdenCompraDocumentoResponseDTO> listar(Long ordenCompraId);

    OrdenCompraDocumentoDescargaDTO descargar(Long documentoId);
}
