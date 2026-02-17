package com.willyes.clemenintegra.bom.controller;

import com.willyes.clemenintegra.bom.dto.DocumentoFormulaDescargaDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaMetadataDTO;
import com.willyes.clemenintegra.bom.dto.DocumentoFormulaResponseDTO;
import com.willyes.clemenintegra.bom.service.DocumentoFormulaService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bom/formulas")
@RequiredArgsConstructor
public class DocumentoFormulaController {
    // TODO(rbac-bom-cut1): retirar fallback por roles y permisos granulares BOM_* legacy al finalizar migracion canonica.

    private final DocumentoFormulaService documentoService;

    @GetMapping("/{formulaId}/documentos")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ')")
    public List<DocumentoFormulaResponseDTO> listarDocumentos(@PathVariable Long formulaId) {
        return documentoService.listarDocumentos(formulaId);
    }

    @PostMapping(value = "/{formulaId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE')")
    public ResponseEntity<DocumentoFormulaResponseDTO> subirDocumento(
            @PathVariable Long formulaId,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart(value = "metadata", required = false) DocumentoFormulaMetadataDTO metadata,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        DocumentoFormulaResponseDTO respuesta = documentoService.guardarDocumento(
                formulaId,
                archivo,
                metadata,
                usuarioAutenticado != null ? usuarioAutenticado.getId() : null);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/documentos/{documentoId}/descargar")
    @PreAuthorize("hasAnyAuthority('BOM_READ','BOM_FORMULA_READ')")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long documentoId) {
        DocumentoFormulaDescargaDTO descarga = documentoService.descargarDocumento(documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + descarga.getNombreArchivo() + "\"")
                .body(descarga.getRecurso());
    }

    @DeleteMapping("/documentos/{documentoId}")
    @PreAuthorize("hasAnyAuthority('BOM_WRITE','BOM_FORMULA_WRITE')")
    public ResponseEntity<Void> eliminarDocumento(
            @PathVariable Long documentoId,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        documentoService.eliminarDocumento(documentoId, usuarioAutenticado != null ? usuarioAutenticado.getId() : null);
        return ResponseEntity.noContent().build();
    }
}
