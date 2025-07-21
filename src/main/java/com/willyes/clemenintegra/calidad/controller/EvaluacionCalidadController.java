package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.service.EvaluacionCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

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

    @GetMapping("/{id}")
    public ResponseEntity<EvaluacionCalidadResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_CALIDAD', 'ROL_ANALISTA_CALIDAD', 'ROL_SUPER_ADMIN')")
    public ResponseEntity<EvaluacionCalidadResponseDTO> crear(
            @ModelAttribute @Valid EvaluacionCalidadRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
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
}
