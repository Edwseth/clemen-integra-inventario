package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.*;
import com.willyes.clemenintegra.calidad.service.EspecificacionesCalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calidad/especificaciones")
@RequiredArgsConstructor
public class EspecificacionesCalidadController {

    private final EspecificacionesCalidadService service;

    @GetMapping("/producto/{productoId}/fisico-quimicas")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<EspecificacionesFisicoQuimicasProductoDTO> listarFisicoQuimicas(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.listarFisicoQuimicasPorProducto(productoId));
    }

    @PostMapping("/producto/{productoId}/fisico-quimicas")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<List<EspecificacionFisicoQuimicaDTO>> crearFisicoQuimicas(
            @PathVariable Long productoId,
            @RequestBody List<EspecificacionFisicoQuimicaRequest> request) {
        return ResponseEntity.ok(service.crearFisicoQuimicas(productoId, request));
    }

    @PutMapping("/fisico-quimicas/{id}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<EspecificacionFisicoQuimicaDTO> actualizarFisicoQuimica(
            @PathVariable Long id,
            @RequestBody EspecificacionFisicoQuimicaRequest request) {
        return ResponseEntity.ok(service.actualizarFisicoQuimica(id, request));
    }

    @DeleteMapping("/fisico-quimicas/{id}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<Void> eliminarFisicoQuimica(@PathVariable Long id) {
        service.eliminarFisicoQuimica(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/producto/{productoId}/micro")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<List<PlantillaAnalisisMicrobiologicoResumenDTO>> listarPlantillasMicro(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.listarPlantillasMicroPorProducto(productoId));
    }

    @GetMapping("/micro")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<List<PlantillaMicrobiologicaReutilizableDTO>> listarTodasLasPlantillasMicro() {
        return ResponseEntity.ok(service.listarTodasLasPlantillasMicro());
    }

    @GetMapping("/micro/{plantillaId}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<PlantillaAnalisisMicrobiologicoDetalleDTO> obtenerDetalleMicro(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.obtenerDetallePlantillaMicro(plantillaId));
    }

    @PostMapping("/producto/{productoId}/micro")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisMicrobiologicoDetalleDTO> crearPlantillaMicro(
            @PathVariable Long productoId,
            @RequestBody PlantillaAnalisisMicrobiologicoRequest request) {
        return ResponseEntity.ok(service.crearPlantillaMicro(productoId, request));
    }

    @PostMapping("/micro/{plantillaId}/clonar-como-nueva-version")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisMicrobiologicoDetalleDTO> clonarPlantillaMicro(@PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.clonarPlantillaMicroComoNuevaVersion(plantillaId));
    }

    @PostMapping("/producto/{productoId}/micro/clonar-desde/{plantillaId}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<PlantillaAnalisisMicrobiologicoDetalleDTO> clonarPlantillaMicroParaProducto(
            @PathVariable Long productoId,
            @PathVariable Long plantillaId) {
        return ResponseEntity.ok(service.clonarPlantillaMicroParaProducto(plantillaId, productoId));
    }
}
