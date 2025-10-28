package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.FefoPreviewResponseDTO;
import com.willyes.clemenintegra.inventario.dto.LoteConsumoDTO;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inventario/fefo")
@RequiredArgsConstructor
public class InventarioFefoController {

    private static final Logger log = LoggerFactory.getLogger(InventarioFefoController.class);

    private final MovimientoInventarioService movimientoInventarioService;

    @GetMapping("/preview")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_SUPER_ADMIN')")
    public ResponseEntity<FefoPreviewResponseDTO> preview(
            @RequestParam(required = false) Long almacenId,
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad) {

        if (productoId == null || productoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCTO_ID_REQUERIDO");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA");
        }

        log.info("FEFO_PREVIEW_REQUEST productoId={} cantidad={} almacenId={}", productoId, cantidad, almacenId);

        List<LoteConsumoDTO> consumos = movimientoInventarioService.simulateFefo(productoId, cantidad, almacenId);
        BigDecimal total = consumos.stream()
                .map(LoteConsumoDTO::getTomar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("FEFO_PREVIEW_RESPONSE productoId={} lotes={} total={}",
                productoId,
                consumos.stream().map(LoteConsumoDTO::getLoteId).toList(),
                total);

        FefoPreviewResponseDTO body = FefoPreviewResponseDTO.builder()
                .consumos(consumos)
                .totalTomar(total)
                .build();
        return ResponseEntity.ok(body);
    }
}
