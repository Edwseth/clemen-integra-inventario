package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.model.ControlEmpaqueLote;
import com.willyes.clemenintegra.produccion.model.ControlProcesoProduccion;
import com.willyes.clemenintegra.produccion.model.ObservacionProceso;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.BatchRecordService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/produccion/batch-record")
@RequiredArgsConstructor
public class BatchRecordController {

    private final BatchRecordService batchRecordService;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    private final ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    private final ObservacionProcesoRepository observacionProcesoRepository;
    private final UsuarioService usuarioService;

    @GetMapping("/{ordenProduccionId}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<BatchRecordDTO> obtener(@PathVariable Long ordenProduccionId) {
        return ResponseEntity.ok(batchRecordService.buildByOrdenProduccion(ordenProduccionId));
    }

    @PostMapping("/{ordenProduccionId}/controles-proceso")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ControlProcesoDTO>> guardarControlesProceso(
            @PathVariable Long ordenProduccionId,
            @RequestBody List<BatchRecordDTO.ControlProcesoDTO> controles) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        controlProcesoProduccionRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ControlProcesoProduccion> entidades = new ArrayList<>();
        for (BatchRecordDTO.ControlProcesoDTO dto : controles != null ? controles : List.<BatchRecordDTO.ControlProcesoDTO>of()) {
            ControlProcesoProduccion entidad = ControlProcesoProduccion.builder()
                    .ordenProduccion(orden)
                    .etapa(dto.etapa)
                    .parametro(dto.parametro)
                    .valorMedido(dto.valorMedido)
                    .unidad(dto.unidad)
                    .cumple(dto.cumple)
                    .observaciones(dto.observaciones)
                    .evaluadoPor(usuario)
                    .build();
            entidades.add(entidad);
        }
        controlProcesoProduccionRepository.saveAll(entidades);
        BatchRecordDTO batchRecordDTO = batchRecordService.buildByOrdenProduccion(ordenProduccionId);
        return ResponseEntity.ok(batchRecordDTO != null ? batchRecordDTO.controlesProceso : List.of());
    }

    @PostMapping("/{ordenProduccionId}/controles-empaque")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ControlEmpaqueDTO>> guardarControlesEmpaque(
            @PathVariable Long ordenProduccionId,
            @RequestBody List<BatchRecordDTO.ControlEmpaqueDTO> controles) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        controlEmpaqueLoteRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ControlEmpaqueLote> entidades = new ArrayList<>();
        for (BatchRecordDTO.ControlEmpaqueDTO dto : controles != null ? controles : List.<BatchRecordDTO.ControlEmpaqueDTO>of()) {
            ControlEmpaqueLote entidad = ControlEmpaqueLote.builder()
                    .ordenProduccion(orden)
                    .parametro(dto.parametro)
                    .valorMedido(dto.valorMedido)
                    .unidad(dto.unidad)
                    .cumple(dto.cumple)
                    .observaciones(dto.observaciones)
                    .evaluadoPor(usuario)
                    .build();
            entidades.add(entidad);
        }
        controlEmpaqueLoteRepository.saveAll(entidades);
        BatchRecordDTO batchRecordDTO = batchRecordService.buildByOrdenProduccion(ordenProduccionId);
        return ResponseEntity.ok(batchRecordDTO != null ? batchRecordDTO.controlesEmpaque : List.of());
    }

    @PostMapping("/{ordenProduccionId}/observaciones")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ObservacionProcesoDTO>> guardarObservaciones(
            @PathVariable Long ordenProduccionId,
            @RequestBody List<BatchRecordDTO.ObservacionProcesoDTO> observaciones) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        observacionProcesoRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ObservacionProceso> entidades = new ArrayList<>();
        for (BatchRecordDTO.ObservacionProcesoDTO dto : observaciones != null ? observaciones : List.<BatchRecordDTO.ObservacionProcesoDTO>of()) {
            ObservacionProceso entidad = ObservacionProceso.builder()
                    .ordenProduccion(orden)
                    .tipo(dto.tipo)
                    .descripcion(dto.descripcion)
                    .registradoPor(usuario)
                    .build();
            entidades.add(entidad);
        }
        observacionProcesoRepository.saveAll(entidades);
        BatchRecordDTO batchRecordDTO = batchRecordService.buildByOrdenProduccion(ordenProduccionId);
        return ResponseEntity.ok(batchRecordDTO != null ? batchRecordDTO.observacionesProceso : List.of());
    }

    @GetMapping(value = "/{ordenProduccionId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable Long ordenProduccionId) {
        batchRecordService.buildByOrdenProduccion(ordenProduccionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=batch-record.pdf");
        // TODO: Implementar renderizado PDF reutilizando el motor de exportación existente.
        return new ResponseEntity<>(new byte[0], headers, HttpStatus.OK);
    }

    private OrdenProduccion obtenerOrden(Long ordenProduccionId) {
        return ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
    }
}
