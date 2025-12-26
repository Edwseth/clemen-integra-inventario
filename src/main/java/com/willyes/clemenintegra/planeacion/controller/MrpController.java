package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mrp/corridas")
@RequiredArgsConstructor
public class MrpController {

    private final MrpService mrpService;
    private final PlanProduccionService planProduccionService;
    private final MrpReporteService mrpReporteService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<CorridaMrpResponseDTO> ejecutar(@RequestBody CorridaMrpRequest request) {
        Optional<PlanProduccionSemanal> plan = planProduccionService.buscarPorId(request.getPlanSemanalId());
        if (plan.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CorridaMrp corrida = mrpService.ejecutarCorridaSemana(plan.get());
        return ResponseEntity.ok(toDto(corrida));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            CorridaMrp corrida = mrpService.obtenerCorrida(id);
            return ResponseEntity.ok(toDto(corrida));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/excel")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<?> exportarExcel(@PathVariable Long id) {
        try {
            byte[] excel = mrpReporteService.generarExcelCorrida(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mrp_corrida_" + id + ".xlsx\"")
                    .body(excel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyAuthority('ROL_COMPRADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<?> exportarPdf(@PathVariable Long id) {
        try {
            byte[] pdf = mrpReporteService.generarPdfCorrida(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mrp_corrida_" + id + ".pdf\"")
                    .body(pdf);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
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
        Producto insumo = detalle.getProducto();
        SugerenciaAbastecimiento sugerencia = detalle.getSugerencia();
        return CorridaMrpResponseDTO.DetalleCorridaMrpDTO.builder()
                .id(detalle.getId())
                .productoId(insumo != null && insumo.getId() != null ? insumo.getId().longValue() : null)
                .productoSku(insumo != null ? insumo.getCodigoSku() : null)
                .productoNombre(insumo != null ? insumo.getNombre() : null)
                .codigoInsumo(insumo != null ? insumo.getCodigoSku() : null)
                .nombreInsumo(insumo != null ? insumo.getNombre() : null)
                .categoriaInsumo(insumo != null && insumo.getCategoriaProducto() != null ? insumo.getCategoriaProducto().getNombre() : null)
                .requerimientoBruto(detalle.getRequerimientoBruto())
                .inventarioDisponible(detalle.getInventarioDisponible())
                .recepcionesProgramadas(detalle.getRecepcionesProgramadas())
                .requerimientoNeto(detalle.getRequerimientoNeto())
                .consumoSemanalPromedio(sugerencia != null ? sugerencia.getConsumoSemanalPromedio() : null)
                .semanasCobertura(sugerencia != null ? sugerencia.getSemanasCobertura() : null)
                .razonesCriticidad(sugerencia != null && sugerencia.getRazonesCriticidad() != null
                        ? sugerencia.getRazonesCriticidad()
                        : List.of())
                .nivelBom(detalle.getNivelBom())
                .mensajeValidacion(detalle.getMensajeValidacion())
                .tipoSugerencia(sugerencia != null && sugerencia.getTipo() != null
                        ? sugerencia.getTipo().name()
                        : null)
                .criticidad(sugerencia != null && sugerencia.getNivelCriticidad() != null
                        ? sugerencia.getNivelCriticidad()
                        : calcularCriticidad(detalle))
                .tipoCambio(detalle.getTipoCambioMrp() != null ? detalle.getTipoCambioMrp().name() : null)
                .build();
    }

    private String calcularCriticidad(DetalleCorridaMrp detalle) {
        BigDecimal neto = detalle.getRequerimientoNeto();
        BigDecimal inventario = Optional.ofNullable(detalle.getInventarioDisponible()).orElse(BigDecimal.ZERO);

        if (neto == null || neto.compareTo(BigDecimal.ZERO) <= 0) {
            return "BAJA";
        }

        if (inventario.compareTo(BigDecimal.ZERO) <= 0) {
            return "ALTA";
        }

        return "MEDIA";
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
                .consumoTotalPeriodo(sugerencia.getConsumoTotalPeriodo())
                .consumoSemanalPromedio(sugerencia.getConsumoSemanalPromedio())
                .semanasCobertura(sugerencia.getSemanasCobertura())
                .nivelCriticidad(sugerencia.getNivelCriticidad())
                .esCritico(sugerencia.getEsCritico())
                .razonesCriticidad(sugerencia.getRazonesCriticidad() != null ? sugerencia.getRazonesCriticidad() : List.of())
                .estado(sugerencia.getEstado() != null ? sugerencia.getEstado().name() : null)
                .build();
    }

    @Data
    public static class CorridaMrpRequest {
        private Long planSemanalId;
    }
}
