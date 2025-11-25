package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mrp/corridas")
@RequiredArgsConstructor
public class MrpController {

    private final MrpService mrpService;
    private final PlanProduccionService planProduccionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION','ROL_COMPRADOR')")
    public ResponseEntity<CorridaMrpResponseDTO> ejecutar(@RequestBody CorridaMrpRequest request) {
        Optional<PlanProduccionSemanal> plan = planProduccionService.buscarPorId(request.getPlanSemanalId());
        if (plan.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CorridaMrp corrida = mrpService.ejecutarCorridaSemana(plan.get());
        return ResponseEntity.ok(toDto(corrida));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES','ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION','ROL_COMPRADOR')")
    public ResponseEntity<CorridaMrpResponseDTO> obtener(@PathVariable Long id) {
        CorridaMrp corrida = mrpService.obtenerCorrida(id);
        return ResponseEntity.ok(toDto(corrida));
    }

    private CorridaMrpResponseDTO toDto(CorridaMrp corrida) {
        List<CorridaMrpResponseDTO.DetalleCorridaMrpDTO> detalles = corrida.getDetalles().stream()
                .map(this::toDetalleDto)
                .toList();

        List<CorridaMrpResponseDTO.SugerenciaAbastecimientoDTO> sugerencias = corrida.getDetalles().stream()
                .map(DetalleCorridaMrp::getSugerencia)
                .filter(s -> s != null)
                .map(this::toSugerenciaDto)
                .collect(Collectors.toList());

        return CorridaMrpResponseDTO.builder()
                .id(corrida.getId())
                .planId(corrida.getPlanProduccionSemanal() != null ? corrida.getPlanProduccionSemanal().getId() : null)
                .fechaEjecucion(corrida.getFechaEjecucion())
                .horizonteInicio(corrida.getHorizonteInicio())
                .horizonteFin(corrida.getHorizonteFin())
                .estado(corrida.getEstado() != null ? corrida.getEstado().name() : null)
                .versionFormulaUsada(corrida.getVersionFormulaUsada())
                .detalles(detalles)
                .sugerencias(sugerencias)
                .build();
    }

    private CorridaMrpResponseDTO.DetalleCorridaMrpDTO toDetalleDto(DetalleCorridaMrp detalle) {
        Producto producto = detalle.getProducto();
        return CorridaMrpResponseDTO.DetalleCorridaMrpDTO.builder()
                .id(detalle.getId())
                .productoId(producto != null && producto.getId() != null ? producto.getId().longValue() : null)
                .productoSku(producto != null ? producto.getCodigoSku() : null)
                .productoNombre(producto != null ? producto.getNombre() : null)
                .requerimientoBruto(detalle.getRequerimientoBruto())
                .inventarioDisponible(detalle.getInventarioDisponible())
                .recepcionesProgramadas(detalle.getRecepcionesProgramadas())
                .requerimientoNeto(detalle.getRequerimientoNeto())
                .nivelBom(detalle.getNivelBom())
                .mensajeValidacion(detalle.getMensajeValidacion())
                .build();
    }

    private CorridaMrpResponseDTO.SugerenciaAbastecimientoDTO toSugerenciaDto(SugerenciaAbastecimiento sugerencia) {
        Producto producto = sugerencia.getDetalleCorrida() != null ? sugerencia.getDetalleCorrida().getProducto() : null;
        return CorridaMrpResponseDTO.SugerenciaAbastecimientoDTO.builder()
                .id(sugerencia.getId())
                .detalleCorridaId(sugerencia.getDetalleCorrida() != null ? sugerencia.getDetalleCorrida().getId() : null)
                .productoId(producto != null && producto.getId() != null ? producto.getId().longValue() : null)
                .productoSku(producto != null ? producto.getCodigoSku() : null)
                .productoNombre(producto != null ? producto.getNombre() : null)
                .tipo(sugerencia.getTipo() != null ? sugerencia.getTipo().name() : null)
                .cantidadSugerida(sugerencia.getCantidadSugerida())
                .fechaNecesidad(sugerencia.getFechaNecesidad())
                .fechaSugeridaLanzamiento(sugerencia.getFechaSugeridaLanzamiento())
                .leadTimeDias(sugerencia.getLeadTimeDias())
                .estado(sugerencia.getEstado() != null ? sugerencia.getEstado().name() : null)
                .build();
    }

    @Data
    public static class CorridaMrpRequest {
        private Long planSemanalId;
    }
}
