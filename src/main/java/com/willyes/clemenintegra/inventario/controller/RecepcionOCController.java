package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.RecepcionOCResponseDTO;
import com.willyes.clemenintegra.inventario.service.RecepcionOCService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/recepciones")
@RequiredArgsConstructor
public class RecepcionOCController {

    private final RecepcionOCService recepcionOCService;

    @Operation(summary = "Obtener una recepción por código (parámetro de consulta)")
    @GetMapping
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<RecepcionOCResponseDTO> obtenerPorCodigoQuery(@RequestParam(name = "codigo") String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODIGO_RECEPCION_REQUERIDO");
        }
        return ResponseEntity.ok(recepcionOCService.obtenerRecepcionPorCodigo(codigo));
    }

    @Operation(summary = "Obtener una recepción por código (segmento en la ruta)")
    @GetMapping("/{codigo}")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_READ')")
    public ResponseEntity<RecepcionOCResponseDTO> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(recepcionOCService.obtenerRecepcionPorCodigo(codigo));
    }
}

