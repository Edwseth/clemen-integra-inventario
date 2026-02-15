package com.willyes.clemenintegra.produccion.controller;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.*;
import com.willyes.clemenintegra.produccion.mapper.ProduccionMapper;
import com.willyes.clemenintegra.produccion.model.*;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.produccion.service.*;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
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
    private final OrdenProduccionRepository ordenProduccionRepository;
    //private final UsuarioService usuarioService;

    // TODO(rbac-prod-cut2): retirar fallback por roles y permisos granulares PROD_* legacy al finalizar migracion canónica.


    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('PROD_EXPORT','PROD_OP_EXPORT','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('PROD_EXPORT','PROD_OP_EXPORT','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
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


    @GetMapping("/lookup")
    // TODO:REMOVE_AFTER_PROD_FULL_MIGRATION
    @PreAuthorize("hasAnyAuthority('PROD_READ','ROL_CONTADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> lookup(@RequestParam(required = false) Long id,
                                                              @RequestParam(required = false) String codigo) {
        String codigoNormalizado = codigo != null ? codigo.trim() : null;
        if (codigo != null && codigoNormalizado.isEmpty()) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe enviar un parámetro válido: codigo o id.");
        }

        if (codigoNormalizado != null && !codigoNormalizado.isEmpty()) {
            return ordenProduccionRepository.findByCodigoOrdenIgnoreCase(codigoNormalizado)
                    .map(ProduccionMapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA,
                            "ORDEN_NO_ENCONTRADA"));
        }

        if (id != null) {
            return service.buscarPorId(id)
                    .map(ProduccionMapper::toResponse)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.ORDEN_PRODUCCION_NO_ENCONTRADA,
                            "ORDEN_NO_ENCONTRADA"));
        }

        throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                "Debe enviar un parámetro válido: codigo o id.");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/trazabilidad")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<ProduccionTrazabilidadResponseDTO> obtenerTrazabilidad(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerTrazabilidad(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PROD_WRITE','PROD_OP_CREATE','ROL_JEFE_PRODUCCION','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<ResultadoValidacionOrdenDTO> crear(@Valid @RequestBody CrearOrdenProduccionRequestDTO request) {
        ResultadoValidacionOrdenDTO resultado = service.crearOrden(request);
        HttpStatus status = resultado.isEsValida() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        if (!resultado.isEsValida()) {
            resultado.setCode("STOCK_INSUFICIENTE");
        }
        return new ResponseEntity<>(resultado, status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PROD_WRITE','PROD_OP_EDIT','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('PROD_WRITE','PROD_OP_EDIT','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('PROD_WORKFLOW_FINISH','PROD_WORKFLOW','PROD_OP_WORKFLOW_FINALIZE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> finalizar(@PathVariable Long id,
                                                               @RequestBody FinalizarOrdenRequestDTO request) {
        OrdenProduccion orden = service.finalizar(id, request.getCantidadProducida());
        return ResponseEntity.ok(ProduccionMapper.toResponse(orden));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('PROD_OP_WORKFLOW_CANCEL','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                         @RequestBody(required = false) CancelarOrdenRequestDTO request) {
        service.cancelarOrden(id, request != null ? request.getMotivo() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cierres")
    @PreAuthorize("hasAnyAuthority('PROD_WORKFLOW_FINISH','PROD_WORKFLOW','PROD_OP_WORKFLOW_FINALIZE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> registrarCierre(@PathVariable Long id,
                                                                     @Valid @RequestBody CierreProduccionRequestDTO request) {
        log.debug("Registrar cierre recibido: ordenId={}, payload={}", id, request);
        service.registrarCierre(id, request);
        OrdenProduccion ordenFull = ordenProduccionRepository.findByIdForCierreResponse(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden de producción no encontrada: " + id));
        return ResponseEntity.ok(ProduccionMapper.toResponse(ordenFull));
    }

    @GetMapping("/{id}/cierres")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public Page<CierreProduccionResponseDTO> listarCierres(@PathVariable Long id,
                                                          @PageableDefault(size = 10, sort = "fechaCierre", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listarCierres(id, pageable);
    }

    @GetMapping("/{id}/etapas")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public List<EtapaProduccionResponse> listarEtapas(@PathVariable Long id) {
        return service.listarEtapas(id);
    }

    @PostMapping("/{id}/etapas/clonar")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CLONE','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<Void> clonarEtapas(@PathVariable Long id) {
        service.clonarEtapas(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{ordenId}/etapas/{etapaId}/iniciar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('PROD_WORKFLOW_START','PROD_WORKFLOW','PROD_ETAPA_START','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<OrdenProduccionResponseDTO> iniciarEtapa(@PathVariable Long ordenId, @PathVariable Long etapaId) {
        service.iniciarEtapa(ordenId, etapaId);
        return service.buscarPorId(ordenId)
                .map(ProduccionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Compatibilidad POST agregada porque el frontend usa POST
    @RequestMapping(value = "/{ordenId}/etapas/{etapaId}/finalizar", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('PROD_WORKFLOW_FINISH','PROD_WORKFLOW','PROD_ETAPA_FINISH','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<EtapaProduccionResponse> finalizarEtapa(@PathVariable Long ordenId, @PathVariable Long etapaId) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();
        return ResponseEntity.ok(ProduccionMapper.toResponse(service.finalizarEtapa(ordenId, etapaId, usuario.getId())));
    }

    @GetMapping("/{id}/insumos")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_ALMACENES','ROL_ALMACENISTA','ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public List<InsumoOPDTO> listarInsumos(@PathVariable Long id) {
        return service.listarInsumos(id);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public Page<MovimientoInventarioResponseDTO> listarMovimientos(@PathVariable Long id,
                                                                   @RequestParam(name = "etapaId", required = false) Long etapaId,
                                                                   Pageable pageable) {
        return service.listarMovimientos(id, etapaId, pageable);
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/consumos")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listarConsumosPorEtapa(@PathVariable Long ordenId,
                                                                                        @PathVariable Long etapaId,
                                                                                        @RequestParam(name = "clasificacion", required = false) ClasificacionMovimientoInventario clasificacion) {
        return ResponseEntity.ok(service.listarConsumosPorEtapa(ordenId, etapaId, clasificacion));
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/movimientos")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listarMovimientosPorEtapa(@PathVariable Long ordenId,
                                                                                           @PathVariable Long etapaId,
                                                                                           @RequestParam(name = "clasificacion", required = false) ClasificacionMovimientoInventario clasificacion) {
        return ResponseEntity.ok(service.listarMovimientosPorEtapa(ordenId, etapaId, clasificacion));
    }

    @GetMapping("/{ordenId}/etapas/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CHECKLIST_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_JEFE_CALIDAD','ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> obtenerChecklistPorEtapa(@PathVariable Long ordenId,
                                                                      @PathVariable Long etapaId) {
        log.info("ChecklistEtapa - GET ordenId={}, etapaId={}", ordenId, etapaId);
        return ResponseEntity.ok(checklistEtapaService.obtenerPorOrdenYEtapa(ordenId, etapaId));
    }

    @PostMapping("/{ordenId}/etapas/{etapaId}/checklist")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CHECKLIST_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistEtapaDTO> actualizarChecklistPorEtapa(@PathVariable Long ordenId,
                                                                         @PathVariable Long etapaId,
                                                                         @RequestBody List<ChecklistItemDTO> items) {
        return ResponseEntity.ok(checklistEtapaService.actualizarEnOrden(ordenId, etapaId, items));
    }

    @PostMapping("/{ordenId}/etapas/{etapaId}/checklist/{itemId}/completar")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CHECKLIST_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDTO> completarChecklistItem(@PathVariable Long ordenId,
                                                                   @PathVariable Long etapaId,
                                                                   @PathVariable Long itemId,
                                                                   @RequestBody(required = false) ChecklistAccionRequestDTO request) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(checklistEtapaService.completarItem(ordenId, etapaId, itemId, observacion));
    }

    @PostMapping("/{ordenId}/etapas/{etapaId}/checklist/{itemId}/no-aplica")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CHECKLIST_WRITE','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDTO> marcarNoAplicaChecklistItem(@PathVariable Long ordenId,
                                                                        @PathVariable Long etapaId,
                                                                        @PathVariable Long itemId,
                                                                        @RequestBody(required = false) ChecklistAccionRequestDTO request) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(checklistEtapaService.marcarNoAplica(ordenId, etapaId, itemId, observacion));
    }

    @PostMapping("/{ordenId}/etapas/{etapaId}/checklist/{itemId}/reabrir")
    @PreAuthorize("hasAnyAuthority('PROD_ETAPA_CHECKLIST_WRITE','ROL_JEFE_PRODUCCION','ROL_SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDTO> reabrirChecklistItem(@PathVariable Long ordenId,
                                                                 @PathVariable Long etapaId,
                                                                 @PathVariable Long itemId) {
        return ResponseEntity.ok(checklistEtapaService.reabrirItem(ordenId, etapaId, itemId));
    }

    @GetMapping("/{id}/lote")
    @PreAuthorize("hasAnyAuthority('PROD_READ','PROD_OP_READ','ROL_JEFE_PRODUCCION','ROL_LIDER_ALIMENTOS','ROL_LIDER_HOMEOPATICOS'," +
            "'ROL_PLANEADOR','ROL_SUPER_ADMIN')")
    public ResponseEntity<LoteProductoResponse> obtenerLote(@PathVariable Long id) {
        LoteProductoResponse lote = service.obtenerLote(id);
        return lote != null ? ResponseEntity.ok(lote) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{ordenId}/backfill-salida")
    @PreAuthorize("hasAnyAuthority('PROD_OP_WORKFLOW_FINALIZE','ROL_SUPER_ADMIN','ROL_JEFE_PRODUCCION')")
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
