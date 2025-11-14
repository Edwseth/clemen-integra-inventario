package com.willyes.clemenintegra.bom.service;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaDescargaDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaMetadataDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoFormulaService {
    List<DocumentoFormulaResponseDTO> listarDocumentos(Long formulaId);

    DocumentoFormulaResponseDTO guardarDocumento(Long formulaId,
                                                 MultipartFile archivo,
                                                 DocumentoFormulaMetadataDTO metadata,
                                                 Long usuarioId);

    DocumentoFormulaDescargaDTO descargarDocumento(Long documentoId);

    void eliminarDocumento(Long documentoId, Long usuarioId);
}
