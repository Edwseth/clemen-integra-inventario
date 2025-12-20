package com.willyes.clemenintegra.documental.controller;

import com.willyes.clemenintegra.documental.dto.DocumentoCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoDetalleDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoEstadoRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionCreateRequest;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDTO;
import com.willyes.clemenintegra.documental.dto.DocumentoVersionDownloadDTO;
import com.willyes.clemenintegra.documental.model.enums.AreaDocumento;
import com.willyes.clemenintegra.documental.model.enums.EstadoDocumento;
import com.willyes.clemenintegra.documental.model.enums.TipoDocumento;
import com.willyes.clemenintegra.documental.service.ControlDocumentalService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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

@RestController
@RequestMapping("/api/documental/documentos")
@RequiredArgsConstructor
public class ControlDocumentalController {

    private final ControlDocumentalService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DocumentoDTO>> buscar(
            @RequestParam(required = false) TipoDocumento tipo,
            @RequestParam(required = false) AreaDocumento area,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.buscarDocumentos(tipo, area, estado, texto, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentoDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerDetalleDocumento(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DocumentoDTO> crear(
            @Valid @RequestBody DocumentoCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        return ResponseEntity.ok(service.crearDocumento(request, usuario));
    }

    @PostMapping(path = "/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<DocumentoVersionDTO> agregarVersion(
            @PathVariable Long id,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart(value = "request", required = false) DocumentoVersionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        return ResponseEntity.ok(service.agregarVersion(id, request, archivo, usuario));
    }

    @GetMapping("/{id}/versiones/{versionId}/archivo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargarArchivo(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        DocumentoVersionDownloadDTO descarga = service.descargarArchivoVersion(id, versionId);
        MediaType mediaType = descarga.contentType() != null
                ? MediaType.parseMediaType(descarga.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + descarga.nombreArchivo() + "\"")
                .body(descarga.recurso());
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody DocumentoEstadoRequest request) {
        service.cambiarEstadoDocumento(id, request.estado);
        return ResponseEntity.noContent().build();
    }
}
