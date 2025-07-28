package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
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

    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_SUPER_ADMIN')")
    @GetMapping("/consolidadas")
    public ResponseEntity<java.util.List<EvaluacionConsolidadaResponseDTO>> getEvaluacionesConsolidadas(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(service.obtenerEvaluacionesConsolidadas(fechaInicio, fechaFin));
    }

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
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadResponseDTO> crear(
            @ModelAttribute @Valid EvaluacionCalidadRequestDTO dto,
            @RequestPart(value = "archivos", required = false) java.util.List<MultipartFile> archivos) {
        return ResponseEntity.ok(service.crear(dto, archivos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD', 'ROL_MICROBIOLOGO', 'ROL_JEFE_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadResponseDTO> actualizar(@PathVariable Long id,
                                                                   @RequestBody EvaluacionCalidadRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archivo/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verArchivo(@PathVariable String nombreArchivo) {
        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones")
                    .toAbsolutePath().normalize();
            Path archivoPath = uploadDir.resolve(nombreArchivo).normalize();

            if (!archivoPath.startsWith(uploadDir) || !Files.exists(archivoPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource recurso = new UrlResource(archivoPath.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String tipoContenido = Files.probeContentType(archivoPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(tipoContenido != null ? tipoContenido : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                    .body(recurso);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
