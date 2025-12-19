package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadCreateRequest;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadResumenDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDownloadDTO;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoCalidadService {

    Page<DocumentoCalidadDTO> listar(DocumentoCalidadTipo tipo, DocumentoCalidadEstado estado, String q, Pageable pageable);

    DocumentoCalidadDTO crear(DocumentoCalidadCreateRequest request, Long usuarioId);

    DocumentoCalidadVersionDTO subirVersion(Long documentoId, MultipartFile archivo, String nombreVisible, Long usuarioId);

    List<DocumentoCalidadVersionDTO> listarVersiones(Long documentoId);

    DocumentoCalidadVersionDownloadDTO descargarVersion(Long versionId);

    DocumentoCalidadDTO obsoletar(Long documentoId, Long usuarioId);

    List<DocumentoCalidadResumenDTO> listarVigentesPorLote(Long loteId);

    List<DocumentoCalidadResumenDTO> listarVigentesPorTipo(DocumentoCalidadTipo tipo);
}
