package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.produccion.dto.AlertaOrdenProduccionDTO;
import com.willyes.clemenintegra.produccion.dto.IndicadoresProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.service.ProduccionIndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class IndicadoresProduccionController {

    private final ProduccionIndicadoresService produccionIndicadoresService;

    @GetMapping("/indicadores")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_AUXILIAR_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<IndicadoresProduccionResponseDTO> obtenerIndicadores(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {
        return ResponseEntity.ok(produccionIndicadoresService.calcularIndicadores(fechaInicio, fechaFin));
    }

    @GetMapping("/ordenes/alertas")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_AUXILIAR_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<AlertaOrdenProduccionDTO>> obtenerAlertas(
            @RequestParam(required = false) LocalDate fechaReferencia,
            @RequestParam(required = false, defaultValue = "3") Integer diasVentana) {
        return ResponseEntity.ok(produccionIndicadoresService.obtenerOrdenesConAlertas(fechaReferencia, diasVentana));
    }
}
