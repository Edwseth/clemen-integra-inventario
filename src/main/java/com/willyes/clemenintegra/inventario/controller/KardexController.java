package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.KardexFiltro;
import com.willyes.clemenintegra.inventario.dto.KardexItemDTO;
import com.willyes.clemenintegra.inventario.service.KardexService;
import com.willyes.clemenintegra.shared.util.DateParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventario/kardex")
@RequiredArgsConstructor
public class KardexController {

    private final KardexService kardexService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('INV_KARDEX_READ')")
    public ResponseEntity<List<KardexItemDTO>> obtenerKardex(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String codigoSku,
            @RequestParam(required = false) Long loteId,
            @RequestParam(required = false) String codigoLote,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ordenProduccionId,
            @RequestParam(required = false) Long etapaProduccionId
    ) {
        if (productoId == null && !StringUtils.hasText(codigoSku)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere productoId o codigoSku");
        }

        LocalDateTime inicio = parseFecha(fechaDesde, true);
        LocalDateTime fin = parseFecha(fechaHasta, false);

        KardexFiltro filtro = KardexFiltro.builder()
                .productoId(productoId)
                .codigoSku(codigoSku)
                .loteId(loteId)
                .codigoLote(codigoLote)
                .fechaDesde(inicio)
                .fechaHasta(fin)
                .almacenId(almacenId)
                .ordenProduccionId(ordenProduccionId)
                .etapaProduccionId(etapaProduccionId)
                .build();

        try {
            List<KardexItemDTO> resultado = kardexService.obtenerKardex(filtro);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private LocalDateTime parseFecha(String fechaTexto, boolean isInicio) {
        if (!StringUtils.hasText(fechaTexto)) {
            return null;
        }
        try {
            return isInicio ? DateParser.parseStart(fechaTexto) : DateParser.parseEnd(fechaTexto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
