package com.willyes.clemenintegra.inventario.regularizacion.controller;

import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.RegularizacionDetalleDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.TipoVariacionDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.variaciones.VariacionOPResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.service.variaciones.VariacionesProduccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/produccion/variaciones")
@RequiredArgsConstructor
public class VariacionesProduccionController {

    private final VariacionesProduccionService variacionesService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROD_VARIACIONES_READ')")
    public ResponseEntity<Page<VariacionOPResponseDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long ordenProduccionId,
            @RequestParam(required = false, defaultValue = "false") boolean soloConVariacion,
            @RequestParam(required = false) TipoVariacionDTO tipoVariacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("fechaIngreso"), Sort.Order.desc("regularizacionId")));
        return ResponseEntity.ok(variacionesService.listarVariaciones(
                fechaInicio,
                fechaFin,
                ordenProduccionId,
                soloConVariacion,
                tipoVariacion,
                pageable
        ));
    }

    @GetMapping("/{regularizacionId}")
    @PreAuthorize("hasAnyAuthority('PROD_VARIACIONES_READ')")
    public ResponseEntity<List<RegularizacionDetalleDTO>> detalle(@PathVariable Long regularizacionId) {
        return ResponseEntity.ok(variacionesService.obtenerDetalle(regularizacionId));
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('PROD_VARIACIONES_EXPORT')")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long ordenProduccionId,
            @RequestParam(required = false, defaultValue = "false") boolean soloConVariacion,
            @RequestParam(required = false) TipoVariacionDTO tipoVariacion) {

        byte[] excel = variacionesService.exportarExcel(
                fechaInicio,
                fechaFin,
                ordenProduccionId,
                soloConVariacion,
                tipoVariacion
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe-variaciones.xlsx");

        return ResponseEntity.ok().headers(headers).body(excel);
    }
}
