package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaListadoDTO;
import com.willyes.clemenintegra.calidad.dto.PlantillaAnalisisMicroDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroRequestDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import com.willyes.clemenintegra.calidad.service.ResultadoAnalisisMicroService;
import com.willyes.clemenintegra.calidad.service.PlantillaAnalisisMicroService;

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
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/calidad/evaluaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
public class EvaluacionCalidadController {

    private final EvaluacionCalidadService service;
    private final ResultadoAnalisisMicroService resultadoAnalisisMicroService;
    private final PlantillaAnalisisMicroService plantillaAnalisisMicroService;

    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    @GetMapping("/consolidadas")
    public ResponseEntity<Page<EvaluacionConsolidadaListadoDTO>> getEvaluacionesConsolidadas(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(service.obtenerEvaluacionesConsolidadas(fechaInicio, fechaFin, pageable));
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
            @PageableDefault(sort = "fechaEvaluacion", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listarPorFecha(fechaInicio, fechaFin, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerDetalle(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadResponseDTO> crear(
            @ModelAttribute @Valid EvaluacionCalidadRequestDTO dto,
            @RequestPart(value = "archivos", required = false) java.util.List<MultipartFile> archivos) {
        return ResponseEntity.ok(service.crear(dto, archivos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD','ROL_ANALISTA_CALIDAD','ROL_MICROBIOLOGO','ROL_SUPER_ADMIN')")
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

    /**
     * Endpoint para registrar o actualizar resultados microbiológicos de una evaluación existente.
     * <ul>
     *     <li><b>Método:</b> POST</li>
     *     <li><b>Ruta:</b> {@code /api/calidad/evaluaciones/{evaluacionId}/resultados-micro}</li>
     *     <li><b>Body:</b> arreglo JSON con objetos {@link ResultadoAnalisisMicroRequestDTO} (parametroId, resultado, cumple,
     *     observaciones).</li>
     * </ul>
     */
    @PostMapping(path = "/{evaluacionId}/resultados-micro")
    @PreAuthorize("hasAnyAuthority('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<java.util.List<ResultadoAnalisisMicroResponseDTO>> guardarResultadosMicro(
            @PathVariable Long evaluacionId,
            @RequestBody java.util.List<ResultadoAnalisisMicroRequestDTO> resultados) {
        return ResponseEntity.ok(resultadoAnalisisMicroService.guardarResultados(evaluacionId, resultados));
    }

    /**
     * Fuente de verdad para frontend: solo si {@code requiereAnalisisMicro} es true y existe plantilla
     * se debe renderizar la tabla de parámetros microbiológicos.
    */
    @GetMapping("/plantillas/micro/producto/{productoId}")
    @PreAuthorize("hasAnyAuthority('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlantillaAnalisisMicroDTO> obtenerPlantillaMicro(@PathVariable Long productoId) {
        PlantillaAnalisisMicroDTO dto = plantillaAnalisisMicroService.obtenerPorProducto(productoId);
        if (dto == null || !dto.isRequiereAnalisisMicro()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping(path = "/{evaluacionId}/resultados-micro")
    @PreAuthorize("hasAnyAuthority('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<java.util.List<ResultadoAnalisisMicroResponseDTO>> obtenerResultadosMicro(@PathVariable Long evaluacionId) {
        return ResponseEntity.ok(resultadoAnalisisMicroService.obtenerPorEvaluacion(evaluacionId));
    }

    @GetMapping(path = "/{evaluacionId}/micro/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_MICROBIOLOGO','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    // Reporte: PDF de análisis microbiológico asociado a una evaluación de calidad.
    public ResponseEntity<byte[]> descargarPdfMicro(@PathVariable Long evaluacionId) {
        byte[] pdf = resultadoAnalisisMicroService.obtenerPdfMicro(evaluacionId);
        String nombreArchivo = "analisis-micro-" + evaluacionId + ".pdf";
        try {
            EvaluacionCalidadResponseDTO dto = service.obtenerPorId(evaluacionId);
            if (dto != null && dto.getNombreLote() != null) {
                nombreArchivo = "analisis-micro-" + dto.getNombreLote() + ".pdf";
            }
        } catch (Exception ignored) {
            // Se mantiene el nombre por defecto si no se puede resolver el lote
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(pdf);
    }

}
