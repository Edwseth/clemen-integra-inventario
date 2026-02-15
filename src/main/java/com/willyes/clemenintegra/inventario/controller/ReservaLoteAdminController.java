package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ReservaLoteRepairResultDTO;
import com.willyes.clemenintegra.inventario.service.ReservaLoteRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/repair")
@RequiredArgsConstructor
public class ReservaLoteAdminController {

    private final ReservaLoteRepairService reservaLoteRepairService;

    @PostMapping("/reservas-lote")
    // TODO:REMOVE_AFTER_INV_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('INV_DECIDE')")
    public ResponseEntity<ReservaLoteRepairResultDTO> repararReservasLote(
            @RequestBody(required = false) ReservaLoteRepairRequestDTO request) {
        return ResponseEntity.ok(reservaLoteRepairService.repararReservasLote(request));
    }
}
