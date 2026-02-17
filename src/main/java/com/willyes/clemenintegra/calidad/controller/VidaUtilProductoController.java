package com.willyes.clemenintegra.calidad.controller;

import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoDTO;
import com.willyes.clemenintegra.calidad.dto.VidaUtilProductoRequest;
import com.willyes.clemenintegra.calidad.service.VidaUtilProductoService;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/calidad/vida-util")
@RequiredArgsConstructor
public class VidaUtilProductoController {

    private final VidaUtilProductoService vidaUtilProductoService;

    @GetMapping("/producto-terminado/{productoId}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<VidaUtilProductoDTO> obtenerPorProducto(@PathVariable Integer productoId) {
        return vidaUtilProductoService.buscarPorProductoId(productoId)
                .map(vida -> ResponseEntity.ok(mapToDto(vida)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VIDA_UTIL_NO_ENCONTRADA"));
    }

    @GetMapping("/productos-terminados")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_READ')")
    public ResponseEntity<Page<VidaUtilProductoDTO>> listar(
            @RequestParam(required = false) String filtro,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "codigoSku") Pageable pageable) {
        String criterio = (search != null && !search.isBlank()) ? search : filtro;
        return ResponseEntity.ok(vidaUtilProductoService.listarProductosTerminados(criterio, pageable));
    }

    @PostMapping("/producto-terminado/{productoId}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<Void> guardar(
            @PathVariable Integer productoId,
            @RequestBody VidaUtilProductoRequest request) {
        vidaUtilProductoService.guardar(productoId, request.getSemanasVigencia());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/producto-terminado/{productoId}")
    // TODO:REMOVE_AFTER_QC_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('QC_WRITE')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer productoId) {
        vidaUtilProductoService.eliminar(productoId);
        return ResponseEntity.noContent().build();
    }

    private VidaUtilProductoDTO mapToDto(VidaUtilProducto vidaUtil) {
        String actualizadoPorNombre = vidaUtil.getActualizadoPor() != null
                ? vidaUtil.getActualizadoPor().getNombreCompleto()
                : null;

        return VidaUtilProductoDTO.builder()
                .productoId(vidaUtil.getProducto().getId())
                .codigoSku(vidaUtil.getProducto().getCodigoSku())
                .nombreProducto(vidaUtil.getProducto().getNombre())
                .semanasVigencia(vidaUtil.getSemanasVigencia())
                .actualizadoPorNombre(actualizadoPorNombre)
                .fechaActualizacion(vidaUtil.getFechaActualizacion())
                .build();
    }
}
