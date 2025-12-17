package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoDescargaDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.OrdenCompraDocumentoUploadForm;
import com.willyes.clemenintegra.inventario.service.OrdenCompraDocumentoService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraDocumentoController {

    private final OrdenCompraDocumentoService documentoService;

    @PostMapping(value = "/{ordenCompraId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public List<OrdenCompraDocumentoResponseDTO> subirDocumentos(
            @PathVariable Long ordenCompraId,
            @RequestPart("archivos") List<MultipartFile> archivos,
            @org.springframework.web.bind.annotation.ModelAttribute OrdenCompraDocumentoUploadForm form,
            @AuthenticationPrincipal CustomUserDetails usuarioAutenticado) {
        return documentoService.subirDocumentos(
                ordenCompraId,
                archivos,
                form != null ? form.getDocumentosAdjuntos() : null,
                usuarioAutenticado != null ? usuarioAutenticado.getId() : null);
    }

    @GetMapping("/{ordenCompraId}/documentos")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public List<OrdenCompraDocumentoResponseDTO> listarDocumentos(@PathVariable Long ordenCompraId) {
        return documentoService.listar(ordenCompraId);
    }

    @GetMapping("/documentos/{documentoId}/download")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN')")
    public ResponseEntity<Resource> descargar(@PathVariable Long documentoId) {
        OrdenCompraDocumentoDescargaDTO descarga = documentoService.descargar(documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + descarga.nombreArchivo() + "\"")
                .body(descarga.recurso());
    }
}
