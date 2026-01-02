package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.service.*;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produccion/ordenes")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrdenProduccionController {

    private final OrdenProduccionService service;
    private final ReporteOrdenProduccionService reporteOrdenProduccionService;
    private final UsuarioService usuarioService;
    private final com.willyes.clemenintegra.inventario.service.MovimientoInventarioService movimientoInventarioService;
    private final ChecklistEtapaService checklistEtapaService;
    //private final UsuarioService usuarioService;


    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public Page<OrdenProduccionResponseDTO> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) EstadoProduccion estado,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @PageableDefault(size = 10, sort = "fechaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarPaginado(codigo, estado, responsable, fechaInicio, fechaFin, pageable);
    }

    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) EstadoProduccion estado,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        List<OrdenProduccion> ordenes = service.listar(codigo, estado, responsable, fechaInicio, fechaFin);
        byte[] excel = reporteOrdenProduccionService.generarExcelOrdenesProduccion(ordenes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ordenes-produccion.xlsx");
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) EstadoProduccion estado,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        List<OrdenProduccion> ordenes = service.listar(codigo, estado, responsable, fechaInicio, fechaFin);
        byte[] pdf = reporteOrdenProduccionService.generarPdfOrdenesProduccion(ordenes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ordenes-produccion.pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<ResultadoValidacionOrdenDTO> crear(@RequestBody OrdenProduccionRequestDTO request) {
        ResultadoValidacionOrdenDTO resultado = service.crearOrden(request);
        HttpStatus status = resultado.isEsValida() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        if (!resultado.isEsValida()) {
            resultado.setCode("STOCK_INSUFICIENTE");
        }
        return new ResponseEntity<>(resultado, status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<ResultadoValidacionOrdenDTO> actualizar(@PathVariable Long id, @RequestBody OrdenProduccionRequestDTO request) {
        return service.buscarPorId(id)
                .map(existente -> {
                    Producto producto = new Producto(); producto.setId(request.getProductoId().intValue());
                    Usuario responsable = new Usuario(); responsable.setId(request.getResponsableId());
                    OrdenProduccion entidad = ProduccionMapper.toEntity(request, producto, responsable);
                    entidad.setId(existente.getId());
                    entidad.setCodigoOrden(existente.getCodigoOrden());
                    ResultadoValidacionOrdenDTO resultado = service.guardarConValidacionStock(entidad);
                    HttpStatus status = resultado.isEsValida() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
                    return new ResponseEntity<>(resultado, status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> finalizar(@PathVariable Long id,
                                                               @RequestBody FinalizarOrdenRequestDTO request) {
        OrdenProduccion orden = service.finalizar(id, request.getCantidadProducida());
        return ResponseEntity.ok(ProduccionMapper.toResponse(orden));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> cancelar(@PathVariable Long id,
                                                               @RequestBody(required = false) CancelarOrdenRequestDTO request) {
        OrdenProduccion orden = service.cancelarOrden(id, request != null ? request.getMotivo() : null);
        return ResponseEntity.ok(ProduccionMapper.toResponse(orden));
    }

    @PostMapping("/{id}/cierres")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> registrarCierre(@PathVariable Long id,
                                                                     @Valid @RequestBody CierreProduccionRequestDTO request) {
        log.debug("Registrar cierre recibido: ordenId={}, payload={}", id, request);
        OrdenProduccion orden = service.registrarCierre(id, request);
        return ResponseEntity.ok(ProduccionMapper.toResponse(orden));
    }

    @GetMapping("/{id}/cierres")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public Page<CierreProduccionResponseDTO> listarCierres(@PathVariable Long id,
                                                          @PageableDefault(size = 10, sort = "fechaCierre", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarCierres(id, pageable);
    }

    @GetMapping("/{id}/etapas")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public List<EtapaProduccionResponse> listarEtapas(@PathVariable Long id) {
        return service.listarEtapas(id);
    }

    @PostMapping("/{id}/etapas/clonar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> clonarEtapas(@PathVariable Long id) {
        service.clonarEtapas(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{ordenId}/etapas/{etapaId}/iniciar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> iniciarEtapa(@PathVariable Long ordenId, @PathVariable Long etapaId) {
        service.iniciarEtapa(ordenId, etapaId);
        return service.buscarPorId(ordenId)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Compatibilidad POST agregada porque el frontend usa POST
    @RequestMapping(value = "/{ordenId}/etapas/{etapaId}/finalizar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<EtapaProduccionResponse> finalizarEtapa(@PathVariable Long ordenId, @PathVariable Long etapaId) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.finalizarEtapa(ordenId, etapaId, usuario.getId())));
    }

    @GetMapping("/{id}/insumos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public List<InsumoOPDTO> listarInsumos(@PathVariable Long id) {
        return service.listarInsumos(id);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public Page<MovimientoInventarioResponseDTO> listarMovimientos(@PathVariable Long id,
                                                                   @RequestParam(name = "etapaId", required = false) Long etapaId,
                                                                   Pageable pageable) {
        return service.listarMovimientos(id, etapaId, pageable);
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/consumos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listarConsumosPorEtapa(@PathVariable Long ordenId,
                                                                                        @PathVariable Long etapaId,
                                                                                        @RequestParam(name = "clasificacion", required = false) ClasificacionMovimientoInventario clasificacion) {
        return ResponseEntity.ok(service.listarConsumosPorEtapa(ordenId, etapaId, clasificacion));
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/movimientos")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listarMovimientosPorEtapa(@PathVariable Long ordenId,
                                                                                           @PathVariable Long etapaId,
                                                                                           @RequestParam(name = "clasificacion", required = false) ClasificacionMovimientoInventario clasificacion) {
        return ResponseEntity.ok(service.listarMovimientosPorEtapa(ordenId, etapaId, clasificacion));
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_JEFE_CALIDAD','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> obtenerChecklistPorEtapa(@PathVariable Long ordenId,
                                                                      @PathVariable Long etapaId) {
        log.info("ChecklistEtapa - GET ordenId={}, etapaId={}", ordenId, etapaId);
        return ResponseEntity.ok(checklistEtapaService.obtenerPorOrdenYEtapa(ordenId, etapaId));
    }

    @PostMapping("/{ordenId}/etapas/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> actualizarChecklistPorEtapa(@PathVariable Long ordenId,
                                                                         @PathVariable Long etapaId,
                                                                         @RequestBody List<ChecklistItemDTO> items) {
        return ResponseEntity.ok(checklistEtapaService.actualizarEnOrden(ordenId, etapaId, items));
    }

    @GetMapping("/{id}/lote")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS','ROL_SUPER_ADMIN')")
    public ResponseEntity<LoteProductoResponse> obtenerLote(@PathVariable Long id) {
        LoteProductoResponse lote = service.obtenerLote(id);
        return lote != null ? ResponseEntity.ok(lote) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{ordenId}/backfill-salida")
    @PreAuthorize("hasAnyAuthority('ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION')")
    public ResponseEntity<Map<String, Object>> backfillSalidaProduccion(@PathVariable Long ordenId) {

        // Obtener el usuario autenticado usando el mismo patrón del resto del backend
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        // Ejecutar consumo idempotente
        movimientoInventarioService.consumirInsumosPorOrden(ordenId, null, usuario.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("ordenId", ordenId);
        body.put("status", "OK");
        body.put("accion", "SALIDA_PRODUCCION_BACKFILL");
        return ResponseEntity.ok(body);
    }


}
