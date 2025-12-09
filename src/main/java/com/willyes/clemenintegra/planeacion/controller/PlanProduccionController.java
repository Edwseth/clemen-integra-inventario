package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planeacion/planes-semanales")
@RequiredArgsConstructor
// El plan semanal es responsabilidad del rol Jefe de Producción.
public class PlanProduccionController {

    private final PlanProduccionService planProduccionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> crearOActualizar(@RequestBody PlanProduccionSemanalDTO dto,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (dto.getCreadoPorId() == null && userDetails != null) {
            dto.setCreadoPorId(userDetails.getId());
        }
        PlanProduccionSemanal plan = planProduccionService.crearOActualizar(dto);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> confirmar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.confirmar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<PlanProduccionSemanalDTO> cerrar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.cerrar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<Page<PlanProduccionSemanalDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaFin,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = "semanaInicio") Pageable pageable) {
        Page<PlanProduccionSemanal> planes = planProduccionService.listar(semanaInicio, semanaFin, estado, pageable);
        List<PlanProduccionSemanalDTO> contenido = planes.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(new PageImpl<>(contenido, pageable, planes.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_COMPRADOR','ROL_SUPER_ADMIN')")
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
        return PlanProduccionSemanalDTO.PlanProduccionDetalleDTO.builder()
                .id(detalle.getId())
                .productoId(producto != null && producto.getId() != null ? producto.getId().longValue() : null)
                .cantidadPlanificada(detalle.getCantidadPlanificada())
                .unidadMedidaId(unidad != null ? unidad.getId() : null)
                .prioridad(detalle.getPrioridad())
                .origenDemanda(detalle.getOrigenDemanda())
                .observacion(detalle.getObservacion())
                .build();
    }
}
