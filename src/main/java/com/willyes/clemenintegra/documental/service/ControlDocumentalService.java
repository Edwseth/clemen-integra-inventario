package com.willyes.clemenintegra.documental.service;

import com.willyes.clemenintegra.documental.dto.DocumentoCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoDetalleDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDownloadDTO;
import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ControlDocumentalService {

    Page<DocumentoDTO> buscarDocumentos(TipoDocumento tipo,
                                       AreaDocumento area,
                                       EstadoDocumento estado,
                                       String texto,
                                       Pageable pageable);

    DocumentoDTO crearDocumento(DocumentoCreateRequest request, Usuario creadoPor);

    DocumentoDetalleDTO obtenerDetalleDocumento(Long documentoId);

    DocumentoVersionDTO agregarVersion(Long documentoId,
                                       DocumentoVersionCreateRequest request,
                                       MultipartFile archivo,
                                       Usuario emitidoPor);

    DocumentoVersionDownloadDTO descargarArchivoVersion(Long documentoId, Long versionId);

    void cambiarEstadoDocumento(Long documentoId, EstadoDocumento nuevoEstado);
}
