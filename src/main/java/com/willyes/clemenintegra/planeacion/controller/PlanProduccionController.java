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
import com.willyes.clemenintegra.produccion.dto.CrearOrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.ResultadoValidacionOrdenDTO;
import com.willyes.clemenintegra.produccion.service.OrdenProduccionService;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import jakarta.validation.Valid;
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

    private final PlanProduccionService planProduccionService;
    private final OrdenProduccionService ordenProduccionService;

    @PostMapping
    @PreAuthorize("hasAuthority('PO_PLAN_SEMANAL_WRITE')")
    public ResponseEntity<PlanProduccionSemanalDTO> crearOActualizar(@RequestBody PlanProduccionSemanalDTO dto,
                                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (dto.getCreadoPorId() == null && userDetails != null) {
            dto.setCreadoPorId(userDetails.getId());
        }
        PlanProduccionSemanal plan = planProduccionService.crearOActualizar(dto);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('PO_PLAN_SEMANAL_WRITE')")
    public ResponseEntity<PlanProduccionSemanalDTO> confirmar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.confirmar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('PO_PLAN_SEMANAL_WRITE')")
    public ResponseEntity<PlanProduccionSemanalDTO> cerrar(@PathVariable Long id) {
        PlanProduccionSemanal plan = planProduccionService.cerrar(id);
        return ResponseEntity.ok(toDto(plan));
    }

    @PostMapping("/{planId}/detalles/{planDetalleId}/generar-op")
    @PreAuthorize("hasAnyAuthority('PO_PLAN_SEMANAL_WRITE','PROD_WRITE','PROD_OP_CREATE')")
    public ResponseEntity<ResultadoValidacionOrdenDTO> generarOrdenProduccionDesdeDetalle(
            @PathVariable Long planId,
            @PathVariable Long planDetalleId,
            @Valid @RequestBody CrearOrdenProduccionRequestDTO request) {
        ResultadoValidacionOrdenDTO resultado = ordenProduccionService
                .crearOrdenDesdePlanSemanal(planId, planDetalleId, request);
        return ResponseEntity.status(resultado.isEsValida() ? 201 : 400).body(resultado);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PO_PLAN_SEMANAL_READ')")
    public ResponseEntity<Page<PlanProduccionResumenDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicioDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicioHasta,
            @RequestParam(required = false) EstadoPlanProduccion estado,
            @PageableDefault(size = 20, sort = "semanaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PlanProduccionResumenDTO> planes = planProduccionService.listar(semanaInicioDesde, semanaInicioHasta, estado, pageable);
        return ResponseEntity.ok(planes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PO_PLAN_SEMANAL_READ')")
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
            String unidadProducto = null;
            if (producto.getUnidadMedida() != null) {
                String simbolo = producto.getUnidadMedida().getSimbolo();
                unidadProducto = (simbolo != null && !simbolo.isBlank())
                        ? simbolo
                        : producto.getUnidadMedida().getNombre();
            }
            productoDto = ProductoResumenDTO.builder()
                    .id(producto.getId() != null ? producto.getId().longValue() : null)
                    .codigoSku(producto.getCodigoSku())
                    .nombre(producto.getNombre())
                    .unidadMedida(unidadProducto)
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
