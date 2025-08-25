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
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION')")
    public ResponseEntity<List<AlertaInventarioResponseDTO>> obtenerAlertas() {
        return ResponseEntity.ok(alertaService.obtenerAlertasInventario());
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION')")
    public ResponseEntity<List<ProductoAlertaResponseDTO>> obtenerProductosConStockBajo() {
        List<ProductoAlertaResponseDTO> alertas = alertaService.obtenerProductosConStockBajo();
        return ResponseEntity.ok(alertas);
    }

    @GetMapping("/productos-vencidos")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION')")
    public ResponseEntity<List<LoteAlertaResponseDTO>> obtenerProductosVencidos() {
        return ResponseEntity.ok(alertaService.obtenerLotesVencidos());
    }

    @GetMapping("/lotes-retenidos-prolongados")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_PRODUCCION', 'ROL_JEFE_CALIDAD')")
    public ResponseEntity<List<LoteEstadoProlongadoResponseDTO>> obtenerLotesEnCuarentenaORetenidosProlongados() {
        return ResponseEntity.ok(alertaService.obtenerLotesRetenidosOCuarentenaProlongados());
    }
}
