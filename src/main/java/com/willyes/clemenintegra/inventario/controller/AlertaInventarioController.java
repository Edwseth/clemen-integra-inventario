package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.service.AlertaInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/alertas")
@RequiredArgsConstructor
public class AlertaInventarioController {

    private final AlertaInventarioService alertaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INV_ALERTAS_READ')")
    public ResponseEntity<List<AlertaInventarioResponseDTO>> obtenerAlertas(
            @RequestParam(value = "diasVencimiento", required = false, defaultValue = "30") Integer diasVencimiento,
            @RequestParam(value = "tipo", required = false) AlertaInventarioTipo tipo,
            @RequestParam(value = "almacenId", required = false) String almacenId) {
        return ResponseEntity.ok(alertaService.obtenerAlertasInventario(diasVencimiento, tipo, parseAlmacenId(almacenId)));
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyAuthority('INV_ALERTAS_READ')")
    public ResponseEntity<List<ProductoAlertaResponseDTO>> obtenerProductosConStockBajo() {
        List<ProductoAlertaResponseDTO> alertas = alertaService.obtenerProductosConStockBajo();
        return ResponseEntity.ok(alertas);
    }

    @GetMapping("/productos-vencidos")
    @PreAuthorize("hasAnyAuthority('INV_ALERTAS_READ')")
    public ResponseEntity<List<LoteAlertaResponseDTO>> obtenerProductosVencidos() {
        return ResponseEntity.ok(alertaService.obtenerLotesVencidos());
    }

    @GetMapping("/lotes-retenidos-prolongados")
    @PreAuthorize("hasAnyAuthority('INV_ALERTAS_READ')")
    public ResponseEntity<List<LoteEstadoProlongadoResponseDTO>> obtenerLotesEnCuarentenaORetenidosProlongados() {
        return ResponseEntity.ok(alertaService.obtenerLotesRetenidosOCuarentenaProlongados());
    }

    private Long parseAlmacenId(String almacenId) {
        if (almacenId == null || almacenId.isBlank()) {
            return null;
        }
        return Long.valueOf(almacenId);
    }
}
