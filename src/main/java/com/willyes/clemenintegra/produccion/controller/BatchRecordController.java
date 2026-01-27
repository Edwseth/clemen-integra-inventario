package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDecisionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ControlEmpaqueRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ControlProcesoRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ObservacionProcesoRequestDTO;
import com.willyes.clemenintegra.produccion.model.ControlEmpaqueLote;
import com.willyes.clemenintegra.produccion.model.ControlProcesoProduccion;
import com.willyes.clemenintegra.produccion.model.ObservacionProceso;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.BatchRecordService;
import com.willyes.clemenintegra.produccion.service.ReporteBatchRecordService;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BatchRecordController {

    private final BatchRecordService batchRecordService;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    private final ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    private final ObservacionProcesoRepository observacionProcesoRepository;
    private final UsuarioService usuarioService;
    private final ReporteBatchRecordService reporteBatchRecordService;

    @GetMapping("/produccion/batch-record/{ordenProduccionId}")
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<BatchRecordDTO> obtener(@PathVariable Long ordenProduccionId) {
        return ResponseEntity.ok(batchRecordService.buildByOrdenProduccion(ordenProduccionId));
    }

    @PostMapping("/produccion/batch-record/{ordenProduccionId}/controles-proceso")
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ControlProcesoDTO>> guardarControlesProceso(
            @PathVariable Long ordenProduccionId,
            @Valid @RequestBody List<@Valid ControlProcesoRequestDTO> controles,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : usuarioService.obtenerUsuarioAutenticado();

        controlProcesoProduccionRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ControlProcesoProduccion> entidades = new ArrayList<>();
        for (ControlProcesoRequestDTO dto : controles != null ? controles : List.<ControlProcesoRequestDTO>of()) {
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

    @PostMapping("/produccion/batch-record/{ordenProduccionId}/controles-empaque")
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ControlEmpaqueDTO>> guardarControlesEmpaque(
            @PathVariable Long ordenProduccionId,
            @Valid @RequestBody List<@Valid ControlEmpaqueRequestDTO> controles,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : usuarioService.obtenerUsuarioAutenticado();

        controlEmpaqueLoteRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ControlEmpaqueLote> entidades = new ArrayList<>();
        for (ControlEmpaqueRequestDTO dto : controles != null ? controles : List.<ControlEmpaqueRequestDTO>of()) {
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

    @PostMapping("/produccion/batch-record/{ordenProduccionId}/observaciones")
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<List<BatchRecordDTO.ObservacionProcesoDTO>> guardarObservaciones(
            @PathVariable Long ordenProduccionId,
            @Valid @RequestBody List<@Valid ObservacionProcesoRequestDTO> observaciones,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        OrdenProduccion orden = obtenerOrden(ordenProduccionId);
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : usuarioService.obtenerUsuarioAutenticado();

        observacionProcesoRepository.deleteByOrdenProduccionId(ordenProduccionId);
        List<ObservacionProceso> entidades = new ArrayList<>();
        for (ObservacionProcesoRequestDTO dto : observaciones != null ? observaciones : List.<ObservacionProcesoRequestDTO>of()) {
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

    @GetMapping(value = "/produccion/batch-record/{ordenProduccionId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_EXPORT','ROL_JEFE_PRODUCCION','ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable Long ordenProduccionId) {
        try {
            BatchRecordDTO batchRecordDTO = batchRecordService.buildByOrdenProduccion(ordenProduccionId);
            byte[] pdf = reporteBatchRecordService.generarPdfBatchRecord(ordenProduccionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = batchRecordDTO != null && batchRecordDTO.op != null && batchRecordDTO.op.codigoOrden != null
                    ? "batch-record-" + batchRecordDTO.op.codigoOrden + ".pdf"
                    : "batch-record-orden-" + ordenProduccionId + ".pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exportando PDF de batch record para la OP {}", ordenProduccionId, e);
            throw e;
        }
    }

    @PreAuthorize("hasAnyAuthority('PROD_BATCH_RECORD_DECIDE','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    @PostMapping("/produccion/calidad/batch-record/{ordenProduccionId}/decision")
    public ResponseEntity<Void> decidirBatchRecord(
            @PathVariable Long ordenProduccionId,
            @Valid @RequestBody BatchRecordDecisionRequestDTO request,
            Authentication auth) {
        log.info("Recibida decisión de batch record para OP {}: {}", ordenProduccionId, request.getDecision());
        batchRecordService.decidirBatchRecord(ordenProduccionId, request, auth);
        return ResponseEntity.noContent().build();
    }

    private OrdenProduccion obtenerOrden(Long ordenProduccionId) {
        return ordenProduccionRepository.findById(ordenProduccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDEN_NO_ENCONTRADA"));
    }
}
