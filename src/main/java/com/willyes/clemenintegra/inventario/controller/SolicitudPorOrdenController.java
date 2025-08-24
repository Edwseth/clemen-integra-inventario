package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.PicklistDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudesPorOrdenDTO;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.service.SolicitudMovimientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.willyes.clemenintegra.shared.util.DateParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping({"/api/inventario/solicitudes", "/api/inventarios/solicitudes"})
@RequiredArgsConstructor
@Slf4j
public class SolicitudPorOrdenController {

    private final SolicitudMovimientoService service;

    @GetMapping({"/por-orden", "/ordenes"})
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    @Operation(summary = "Listar solicitudes agrupadas por orden de producción")
    public ResponseEntity<Page<SolicitudesPorOrdenDTO>> listarPorOrden(
            @Parameter(description = "Estado de las solicitudes. Si se omite, se usa PENDIENTE por defecto")
            @RequestParam(required = false) EstadoSolicitudMovimiento estado,
            @Parameter(description = "Filtrar desde esta fecha (inclusive)")
            @RequestParam(required = false) String fechaDesde,
            @Parameter(description = "Filtrar hasta esta fecha (inclusive)")
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaOrden,desc") String sort
    ) {
        LocalDateTime inicio = null;
        LocalDateTime fin = null;
        if ((fechaDesde != null && fechaHasta == null) || (fechaDesde == null && fechaHasta != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren fechaDesde y fechaHasta");
        }
        try {
            if (fechaDesde != null) {
                inicio = DateParser.parseStart(fechaDesde);
                fin = DateParser.parseEnd(fechaHasta);
                if (inicio.isAfter(fin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaDesde no puede ser mayor a fechaHasta");
                }
            }
            String[] partes = sort.split(",");
            Sort sortObj = partes.length == 2
                    ? Sort.by(Sort.Direction.fromString(partes[1]), partes[0])
                    : Sort.by(partes[0]);
            Pageable pageable = PageRequest.of(page, size, sortObj);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                log.debug("listarPorOrden invocado por {} con authorities {}", auth.getName(), auth.getAuthorities());
            }
            return ResponseEntity.ok(service.listGroupByOrden(estado, inicio, fin, pageable));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/orden/{ordenId}")
    @PreAuthorize("hasAnyAuthority('ROL_ALMACENISTA','ROL_JEFE_ALMACENES','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<SolicitudesPorOrdenDTO> obtenerPorOrden(@PathVariable Long ordenId) {
        return ResponseEntity.ok(service.obtenerPorOrden(ordenId));
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
