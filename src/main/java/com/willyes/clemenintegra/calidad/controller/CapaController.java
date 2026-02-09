package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.CapaArchivoDTO;
import com.willyes.clemenintegra.calidad.dto.CapaArchivoDescargaDTO;
import com.willyes.clemenintegra.calidad.dto.CapaDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.service.CapaService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/capas")
@RequiredArgsConstructor
public class CapaController {

    private final CapaService service;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<CapaDTO>> listar(
            @RequestParam(required = false) EstadoCapa estado,
            @RequestParam(required = false) SeveridadNoConformidad severidad,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(estado, severidad, pageable));
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<CapaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<CapaDTO> crear(@Valid @RequestBody CapaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<CapaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody CapaDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @PatchMapping("/{id}/cerrar")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<CapaDTO> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cerrar(id));
    }

    @PostMapping(path = "/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<CapaArchivoDTO> adjuntarArchivo(@PathVariable Long id,
                                                          @RequestPart("archivo") MultipartFile archivo,
                                                          @RequestPart(value = "nombreVisible", required = false) String nombreVisible,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails != null ? userDetails.getId() : null;
        CapaArchivoDTO respuesta = service.adjuntarArchivo(id, archivo, nombreVisible, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}/archivos")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<CapaArchivoDTO>> listarArchivos(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarArchivos(id));
    }

    @GetMapping({"/{capaId}/archivos/{archivoId}/descargar", "/{capaId}/archivos/{archivoId}"})
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<ByteArrayResource> descargarArchivo(@PathVariable Long capaId, @PathVariable Long archivoId) {
        CapaArchivoDescargaDTO archivo = service.descargarArchivo(capaId, archivoId);
        MediaType mediaType = archivo.getContentType() != null
                ? MediaType.parseMediaType(archivo.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getNombreArchivo() + "\"")
                .body(new ByteArrayResource(archivo.getContenido()));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
