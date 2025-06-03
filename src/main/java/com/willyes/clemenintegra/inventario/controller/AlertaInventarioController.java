package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.service.AlertaInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/alertas")
@RequiredArgsConstructor
public class AlertaInventarioController {

    private final AlertaInventarioService alertaService;

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoAlertaResponseDTO>> obtenerProductosConStockBajo() {
        List<ProductoAlertaResponseDTO> alertas = alertaService.obtenerProductosConStockBajo();
        return ResponseEntity.ok(alertas);
    }

    @GetMapping("/productos-vencidos")
    public ResponseEntity<List<LoteAlertaResponseDTO>> obtenerProductosVencidos() {
        return ResponseEntity.ok(alertaService.obtenerLotesVencidos());
    }

    @GetMapping("/lotes-retenidos-prolongados")
    public ResponseEntity<List<LoteEstadoProlongadoResponseDTO>> obtenerLotesEnCuarentenaORetenidosProlongados() {
        return ResponseEntity.ok(alertaService.obtenerLotesRetenidosOCuarentenaProlongados());
    }
}
