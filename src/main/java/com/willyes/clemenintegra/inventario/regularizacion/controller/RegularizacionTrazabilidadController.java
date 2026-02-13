package com.willyes.clemenintegra.inventario.regularizacion.controller;

import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadResponseDTO;
import com.willyes.clemenintegra.inventario.regularizacion.service.RegularizacionTrazabilidadService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/regularizaciones-trazabilidad")
@RequiredArgsConstructor
public class RegularizacionTrazabilidadController {

    private final RegularizacionTrazabilidadService regularizacionTrazabilidadService;

    @PostMapping("/op")
    @PreAuthorize("hasAnyAuthority('ROL_CONTADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<RegularizacionTrazabilidadResponseDTO> regularizarPorOp(
            @RequestBody @Valid RegularizacionTrazabilidadRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Idempotency-Key es obligatorio");
        }
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        RegularizacionTrazabilidadResponseDTO response =
                regularizacionTrazabilidadService.regularizarPorOP(request, idempotencyKey, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
