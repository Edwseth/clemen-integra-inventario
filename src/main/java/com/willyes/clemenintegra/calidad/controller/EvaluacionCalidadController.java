package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/calidad/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionCalidadController {

    private final EvaluacionCalidadService service;

    @GetMapping
    public ResponseEntity<Page<EvaluacionCalidadResponseDTO>> listar(
            @RequestParam(required = false) ResultadoEvaluacion resultado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.listar(resultado, pageable));
    }

    @GetMapping("/evaluaciones")
    public ResponseEntity<Page<EvaluacionCalidadResponseDTO>> listarPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Pageable pageable) {
        return ResponseEntity.ok(service.listarPorFecha(fechaInicio, fechaFin, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadResponseDTO> crear(
            @ModelAttribute @Valid EvaluacionCalidadRequestDTO dto,
            @RequestPart(value = "archivoAdjunto", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(service.crear(dto, archivo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> actualizar(@PathVariable Long id,
                                                                   @RequestBody EvaluacionCalidadRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archivo/{nombreArchivo}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable String nombreArchivo) throws IOException {
        Path archivoPath = Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones", nombreArchivo);
        if (!Files.exists(archivoPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new UrlResource(archivoPath.toUri());
        String tipoContenido = "application/pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipoContenido != null ? tipoContenido : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(recurso);
    }

}
