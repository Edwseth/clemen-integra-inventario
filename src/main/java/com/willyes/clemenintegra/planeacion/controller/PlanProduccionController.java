package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoResumenDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/planeacion/planes-semanales")
@RequiredArgsConstructor
public class PlanProduccionController {

    // TODO(rbac): retirar fallback por rol cuando todos los perfiles PO usen permisos canónicos.
    private static final String PO_ROLE_FALLBACK = "'ROL_JEFE_PRODUCCION','ROL_PLANEADOR','ROL_COMPRADOR','ROL_CONTADOR','ROL_SUPER_ADMIN'";

    private final PlanProduccionService planProduccionService;

    @PostMapping
    // TODO:REMOVE_AFTER_PO_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PO_WRITE','PO_WORKFLOW','PO_WORKFLOW_START','PO_PLAN_SEMANAL_WRITE','" +
            "'ROL_JEFE_PRODUCCION','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> crearOActualizar(@RequestBody PlanProduccionSemanalDTO dto,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (dto.getCreadoPorId() == null && userDetails != null) {
            dto.setCreadoPorId(userDetails.getId());
        }
        PlanProduccionSemanal plan = planProduccionService.crearOActualizar(dto);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/confirmar")
    // TODO:REMOVE_AFTER_PO_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PO_DECIDE','PO_WORKFLOW','PO_WORKFLOW_FINISH','PO_PLAN_SEMANAL_WRITE','" +
            "'ROL_JEFE_PRODUCCION','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> confirmar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.confirmar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/cerrar")
    // TODO:REMOVE_AFTER_PO_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PO_DECIDE','PO_WORKFLOW','PO_WORKFLOW_FINISH','PO_PLAN_SEMANAL_WRITE','" +
            "'ROL_JEFE_PRODUCCION','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> cerrar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.cerrar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @GetMapping
    // TODO:REMOVE_AFTER_PO_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PO_READ','PO_PLAN_SEMANAL_READ'," + PO_ROLE_FALLBACK + ")")
    public ResponseEntity<Page<PlanProduccionResumenDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicioDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicioHasta,
            @RequestParam(required = false) EstadoPlanProduccion estado,
            @PageableDefault(size = 20, sort = "semanaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PlanProduccionResumenDTO> planes = planProduccionService.listar(semanaInicioDesde, semanaInicioHasta, estado, pageable);
        return ResponseEntity.ok(planes);
    }

    @GetMapping("/{id}")
    // TODO:REMOVE_AFTER_PO_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PO_READ','PO_PLAN_SEMANAL_READ'," + PO_ROLE_FALLBACK + ")")
    public ResponseEntity<PlanProduccionSemanalDTO> obtener(@PathVariable Long id) {
        return planProduccionService.buscarPorId(id)
                .map(plan -> ResponseEntity.ok(toDto(plan)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PlanProduccionSemanalDTO toDto(PlanProduccionSemanal plan) {
        List<PlanProduccionSemanalDTO.PlanProduccionDetalleDTO> detalles = plan.getDetalles().stream()
                .map(this::toDetalleDto)
                .toList();
        return PlanProduccionSemanalDTO.builder()
                .id(plan.getId())
                .semanaInicio(plan.getSemanaInicio())
                .semanaFin(plan.getSemanaFin())
                .estado(plan.getEstado() != null ? plan.getEstado().name() : null)
                .comentarios(plan.getComentarios())
                .creadoPorId(plan.getCreadoPor() != null ? plan.getCreadoPor().getId() : null)
                .detalles(detalles)
                .build();
    }

    private PlanProduccionSemanalDTO.PlanProduccionDetalleDTO toDetalleDto(PlanProduccionDetalle detalle) {
        Producto producto = detalle.getProducto();
        UnidadMedida unidad = detalle.getUnidadMedida();
        ProductoResumenDTO productoDto = null;
        if (producto != null) {
            productoDto = ProductoResumenDTO.builder()
                    .id(producto.getId() != null ? producto.getId().longValue() : null)
                    .codigoSku(producto.getCodigoSku())
                    .nombre(producto.getNombre())
                    .build();
        }

        UnidadMedidaResponseDTO unidadDto = null;
        if (unidad != null) {
            unidadDto = UnidadMedidaResponseDTO.builder()
                    .id(unidad.getId())
                    .nombre(unidad.getNombre())
                    .simbolo(unidad.getSimbolo())
                    .build();
        }
        return PlanProduccionSemanalDTO.PlanProduccionDetalleDTO.builder()
                .id(detalle.getId())
                .productoId(producto != null && producto.getId() != null ? producto.getId().longValue() : null)
                .cantidadPlanificada(detalle.getCantidadPlanificada())
                .unidadMedidaId(unidad != null ? unidad.getId() : null)
                .prioridad(detalle.getPrioridad())
                .origenDemanda(detalle.getOrigenDemanda())
                .observacion(detalle.getObservacion())
                .producto(productoDto)
                .unidadMedida(unidadDto)
                .build();
    }
}
