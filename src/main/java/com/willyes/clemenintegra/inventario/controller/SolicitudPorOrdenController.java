package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.PicklistDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudesPorOrdenDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/inventario/solicitudes")
@RequiredArgsConstructor
public class SolicitudPorOrdenController {

    private final SolicitudMovimientoService service;

    @GetMapping("/por-orden")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<SolicitudesPorOrdenDTO>> listarPorOrden(
            @RequestParam(required = false) EstadoSolicitudMovimiento estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(23, 59, 59) : null;
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.listGroupByOrden(estado, desde, hasta, pageable));
    }

    @GetMapping("/orden/{ordenId}/picklist")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> generarPicklist(@PathVariable Long ordenId,
                                                  @RequestParam(defaultValue = "false") boolean incluirAprobadas) {
        PicklistDTO dto = service.generarPicklist(ordenId, incluirAprobadas);
        String filename = "picklist_OP-" + dto.getCodigoOrden() + "_" +
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now()) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(dto.getArchivo());
    }
}
