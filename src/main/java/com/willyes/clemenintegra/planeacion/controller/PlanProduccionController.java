package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.dto.ProductoResumenDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.VidaUtilProducto;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.VidaUtilProductoRepository;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionSemanalDTO;
import com.willyes.clemenintegra.planeacion.dto.PlanProduccionResumenDTO;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.produccion.dto.CrearOrdenProduccionRequestDTO;
import com.willyes.clemenintegra.produccion.dto.CorridaOrdenProduccionResponseDTO;
import com.willyes.clemenintegra.produccion.dto.EjecutarCorridaOpHomeopaticaRequestDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planeacion/planes-semanales")
@RequiredArgsConstructor
public class PlanProduccionController {
    private static final int SEMANAS_HOMEOPATICO = 78;

    private final PlanProduccionService planProduccionService;
    private final OrdenProduccionService ordenProduccionService;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final VidaUtilProductoRepository vidaUtilProductoRepository;

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

    @PostMapping("/{planId}/detalles/{planDetalleId}/generar-op-corrida")
    @PreAuthorize("hasAnyAuthority('PO_PLAN_SEMANAL_WRITE','PROD_WRITE','PROD_OP_CREATE')")
    public ResponseEntity<CorridaOrdenProduccionResponseDTO> ejecutarCorridaHomeopaticaDesdeDetalle(
            @PathVariable Long planId,
            @PathVariable Long planDetalleId,
            @Valid @RequestBody EjecutarCorridaOpHomeopaticaRequestDTO request) {
        CorridaOrdenProduccionResponseDTO response = ordenProduccionService
                .ejecutarCorridaHomeopaticaDesdePlanSemanal(planId, planDetalleId, request);
        return ResponseEntity.status(201).body(response);
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
        Map<Long, List<OrdenProduccion>> opsPorDetalle = obtenerOpsPorDetalle(plan);
        Map<Integer, Integer> semanasVigenciaPorProducto = obtenerSemanasVigenciaPorProducto(plan);
        List<PlanProduccionSemanalDTO.PlanProduccionDetalleDTO> detalles = plan.getDetalles().stream()
                .map(detalle -> toDetalleDto(detalle,
                        opsPorDetalle.getOrDefault(detalle.getId(), List.of()),
                        semanasVigenciaPorProducto))
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

    private PlanProduccionSemanalDTO.PlanProduccionDetalleDTO toDetalleDto(
            PlanProduccionDetalle detalle,
            List<OrdenProduccion> opsDetalle,
            Map<Integer, Integer> semanasVigenciaPorProducto) {
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

        TipoCategoria tipoCategoria = producto != null && producto.getCategoriaProducto() != null
                ? producto.getCategoriaProducto().getTipo()
                : null;
        String tipoProducto = mapTipoProducto(tipoCategoria);
        Integer semanasVigencia = producto != null ? semanasVigenciaPorProducto.get(producto.getId()) : null;
        boolean esHomeopatico = "PT".equals(tipoProducto) && Objects.equals(semanasVigencia, SEMANAS_HOMEOPATICO);
        int totalOpAsociadas = opsDetalle != null ? opsDetalle.size() : 0;
        BigDecimal cantidadProgramadaEnOp = opsDetalle == null ? BigDecimal.ZERO : opsDetalle.stream()
                .map(OrdenProduccion::getCantidadProgramada)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cantidadPlanificada = detalle.getCantidadPlanificada() != null ? detalle.getCantidadPlanificada() : BigDecimal.ZERO;
        BigDecimal cantidadPendiente = cantidadPlanificada.subtract(cantidadProgramadaEnOp);
        if (cantidadPendiente.compareTo(BigDecimal.ZERO) < 0) {
            cantidadPendiente = BigDecimal.ZERO;
        }
        List<Long> opIds = opsDetalle == null ? List.of() : opsDetalle.stream()
                .map(OrdenProduccion::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<String> opCodigos = opsDetalle == null ? List.of() : opsDetalle.stream()
                .map(OrdenProduccion::getCodigoOrden)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();

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
                .tipoProducto(tipoProducto)
                .esHomeopatico(esHomeopatico)
                .totalOpAsociadas(totalOpAsociadas)
                .cantidadTotalProgramadaEnOp(cantidadProgramadaEnOp)
                .cantidadPendiente(cantidadPendiente)
                .opIds(opIds)
                .opCodigos(opCodigos)
                .modoAccionSugerido(determinarModoAccion(tipoProducto, esHomeopatico, totalOpAsociadas, cantidadPendiente))
                .build();
    }

    private String determinarModoAccion(String tipoProducto, boolean esHomeopatico, int totalOpAsociadas, BigDecimal cantidadPendiente) {
        boolean hayPendiente = cantidadPendiente != null && cantidadPendiente.compareTo(BigDecimal.ZERO) > 0;
        if ("PS".equals(tipoProducto)) {
            return totalOpAsociadas == 0 ? "UNICA" : "BLOQUEADA";
        }
        if ("PT".equals(tipoProducto) && !esHomeopatico) {
            return totalOpAsociadas == 0 ? "UNICA" : "BLOQUEADA";
        }
        if ("PT".equals(tipoProducto) && esHomeopatico) {
            return hayPendiente ? "CORRIDA" : "BLOQUEADA";
        }
        return "BLOQUEADA";
    }

    private String mapTipoProducto(TipoCategoria tipoCategoria) {
        if (tipoCategoria == TipoCategoria.PRODUCTO_TERMINADO) {
            return "PT";
        }
        if (tipoCategoria == TipoCategoria.PRODUCTO_SEMI_ELABORADO) {
            return "PS";
        }
        return tipoCategoria != null ? tipoCategoria.name() : null;
    }

    private Map<Long, List<OrdenProduccion>> obtenerOpsPorDetalle(PlanProduccionSemanal plan) {
        Set<Long> detalleIds = plan.getDetalles().stream()
                .map(PlanProduccionDetalle::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (detalleIds.isEmpty()) {
            return Map.of();
        }
        return ordenProduccionRepository.findByPlanProduccionDetalleIdIn(detalleIds).stream()
                .filter(op -> op.getPlanProduccionDetalle() != null && op.getPlanProduccionDetalle().getId() != null)
                .collect(Collectors.groupingBy(op -> op.getPlanProduccionDetalle().getId()));
    }

    private Map<Integer, Integer> obtenerSemanasVigenciaPorProducto(PlanProduccionSemanal plan) {
        Set<Integer> productoIds = plan.getDetalles().stream()
                .map(PlanProduccionDetalle::getProducto)
                .filter(Objects::nonNull)
                .map(Producto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (productoIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Integer> result = new HashMap<>();
        for (VidaUtilProducto vidaUtil : vidaUtilProductoRepository.findAllById(productoIds)) {
            result.put(vidaUtil.getProductoId(), vidaUtil.getSemanasVigencia());
        }
        return result;
    }
}
