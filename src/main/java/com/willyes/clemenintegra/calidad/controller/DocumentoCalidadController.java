package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadCreateRequest;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDTO;
import com.willyes.clemenintegra.calidad.dto.DocumentoCalidadVersionDownloadDTO;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadEstado;
import com.willyes.clemenintegra.calidad.model.enums.DocumentoCalidadTipo;
import com.willyes.clemenintegra.calidad.service.DocumentoCalidadService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/documentos")
@RequiredArgsConstructor
public class DocumentoCalidadController {

    private final DocumentoCalidadService service;

    @GetMapping
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<Page<DocumentoCalidadDTO>> listar(
            @RequestParam(required = false) DocumentoCalidadTipo tipo,
            @RequestParam(required = false) DocumentoCalidadEstado estado,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(tipo, estado, q, pageable));
    }

    @PostMapping
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<DocumentoCalidadDTO> crear(
            @Valid @RequestBody DocumentoCalidadCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(service.crear(request, usuarioId));
    }

    @PostMapping(path = "/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<DocumentoCalidadVersionDTO> subirVersion(
            @PathVariable Long id,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart(value = "nombreVisible", required = false) String nombreVisible,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(service.subirVersion(id, archivo, nombreVisible, usuarioId));
    }

    @GetMapping("/{id}/versiones")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<List<DocumentoCalidadVersionDTO>> listarVersiones(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarVersiones(id));
    }

    @GetMapping("/versiones/{versionId}/download")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_EXPORT')")
    public ResponseEntity<ByteArrayResource> descargarVersion(@PathVariable Long versionId) {
        DocumentoCalidadVersionDownloadDTO descarga = service.descargarVersion(versionId);
        MediaType mediaType = descarga.getContentType() != null
                ? MediaType.parseMediaType(descarga.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + descarga.getNombreArchivo() + "\"")
                .body(new ByteArrayResource(descarga.getContenido()));
    }

    @PatchMapping("/{id}/obsoletar")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_DECIDE')")
    public ResponseEntity<DocumentoCalidadDTO> obsoletar(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(service.obsoletar(id, usuarioId));
    }
}
