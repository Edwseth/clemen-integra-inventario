package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.*;
import com.willyes.clemenintegra.calidad.model.enums.TipoAnalisisPlantilla;
import com.willyes.clemenintegra.calidad.service.PlantillasAnalisisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/plantillas-analisis")
@RequiredArgsConstructor
public class PlantillasAnalisisController {

    private final PlantillasAnalisisService service;

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<List<PlantillaAnalisisResumenDTO>> listarPorProductoYTipo(
            @PathVariable Long productoId,
            @RequestParam TipoAnalisisPlantilla tipoAnalisis) {
        return ResponseEntity.ok(service.listarPorProductoYTipo(productoId, tipoAnalisis));
    }

    @GetMapping("/{plantillaId}")
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<PlantillaAnalisisDetalleDTO> obtenerDetalle(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.obtenerDetalle(plantillaId));
    }

    @PostMapping("/producto/{productoId}")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisDetalleDTO> crearPlantilla(
            @PathVariable Long productoId,
            @RequestParam TipoAnalisisPlantilla tipoAnalisis,
            @RequestBody(required = false) PlantillaAnalisisCreateRequest request) {
        return ResponseEntity.ok(service.crearPlantilla(productoId, tipoAnalisis, request));
    }

    @PostMapping("/{plantillaId}/clonar-como-nueva-version")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisDetalleDTO> clonarComoNuevaVersion(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.clonarComoNuevaVersion(plantillaId));
    }

    @PatchMapping("/{plantillaId}/vigente")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisDetalleDTO> marcarVigente(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.marcarVigente(plantillaId));
    }

    @PostMapping("/{plantillaId}/campos")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaCampoDTO> crearCampo(@PathVariable Long plantillaId,
                                                        @RequestBody PlantillaCampoRequest request) {
        return ResponseEntity.ok(service.crearCampo(plantillaId, request));
    }

    @PutMapping("/{plantillaId}/campos/{campoId}")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaCampoDTO> editarCampo(@PathVariable Long plantillaId,
                                                         @PathVariable Long campoId,
                                                         @RequestBody PlantillaCampoRequest request) {
        return ResponseEntity.ok(service.editarCampo(plantillaId, campoId, request));
    }

    @DeleteMapping("/{plantillaId}/campos/{campoId}")
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<Void> eliminarCampo(@PathVariable Long plantillaId,
                                              @PathVariable Long campoId) {
        service.eliminarCampo(plantillaId, campoId);
        return ResponseEntity.noContent().build();
    }
}
