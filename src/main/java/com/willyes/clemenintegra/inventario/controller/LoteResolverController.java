package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoPorLoteDTO;
import com.willyes.clemenintegra.inventario.service.LoteProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/lotes")
@RequiredArgsConstructor
public class LoteResolverController {

    private final LoteProductoService loteProductoService;

    @GetMapping("/resolver-producto")
    @PreAuthorize("hasAnyAuthority(" +
            "'INV_LOTES_READ'," +
            "'ROL_JEFE_ALMACENES'," +
            "'ROL_ALMACENISTA'," +
            "'ROL_JEFE_PRODUCCION'," +
            "'ROL_LIDER_ALIMENTOS'," +
            "'ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD'," +
            "'ROL_CONTADOR'," +
            "'ROL_SUPER_ADMIN'" +
            ")")
    public ResponseEntity<ProductoPorLoteDTO> resolverProducto(@RequestParam String codigoLote,
                                                               @RequestParam Long ordenProduccionId) {
        ProductoPorLoteDTO dto = loteProductoService.resolverProductoPorLote(codigoLote, ordenProduccionId);
        return ResponseEntity.ok(dto);
    }
}
