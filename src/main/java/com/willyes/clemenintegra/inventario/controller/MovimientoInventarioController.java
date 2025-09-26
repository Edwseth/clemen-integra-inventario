package com.willyes.clemenintegra.inventario.controller;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.SolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.StockQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.willyes.clemenintegra.shared.util.PaginationUtil;
import com.willyes.clemenintegra.shared.util.DateParser;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MovimientoInventarioController.class);
    private final MovimientoInventarioService service;
    private final ProductoRepository productoRepo;
    private final LoteProductoRepository loteRepo;
    private final SolicitudMovimientoRepository solicitudMovimientoRepository;
    private final StockQueryService stockQueryService;
    private final InventoryCatalogResolver inventoryCatalogResolver;

    @Operation(summary = "Registrar un movimiento de inventario")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado correctamente")
    @ApiResponse(responseCode = "400", description = "Solicitud malformada o inválida")
    @ApiResponse(responseCode = "404", description = "Producto o lote no encontrado")
    @ApiResponse(responseCode = "409", description = "No hay suficiente stock disponible")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody @Valid MovimientoInventarioDTO dto) {
        dto = normalizarMovimientoDto(dto);
        try {
            int atenciones = dto.atenciones() != null ? dto.atenciones().size() : 0;
            log.debug("MOV-CONTROLLER registrar solicitudId={} atenciones={} tipo={} clasificacion={} producto={} lote={}",
                    dto.solicitudMovimientoId(), atenciones, dto.tipoMovimiento(),
                    dto.clasificacionMovimientoInventario(), dto.productoId(), dto.loteProductoId());

            // 1) Validación de stock para salidas
            var tipo = dto.tipoMovimiento();
            boolean isSalida = tipo == TipoMovimiento.SALIDA || tipo == TipoMovimiento.AJUSTE;
            boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());
            boolean atencionesVacias = dto.atenciones() == null || dto.atenciones().isEmpty();
            Long detalleSalidaPtId = inventoryCatalogResolver.isSalidaPtEnabled()
                    ? inventoryCatalogResolver.getTipoDetalleSalidaPtId()
                    : null;
            if (detalleSalidaPtId == null) {
                detalleSalidaPtId = inventoryCatalogResolver.getTipoDetalleSalidaId();
            }
            boolean esSalidaPt = inventoryCatalogResolver.isSalidaPtEnabled()
                    && dto.tipoMovimiento() == TipoMovimiento.SALIDA
                    && detalleSalidaPtId != null
                    && Objects.equals(dto.tipoMovimientoDetalleId(), detalleSalidaPtId);
            boolean permitirLoteNulo = dto.loteProductoId() == null && autoSplitSolicitado && atencionesVacias;
            SolicitudMovimiento solicitudMovimiento = null;

            if (isSalida && permitirLoteNulo) {
                log.debug("MOV-CONTROLLER skip stock pre-check autoSplit={} atencionesVacias={} esSalidaPt={}",
                        autoSplitSolicitado, atencionesVacias, esSalidaPt);
            } else if (isSalida) {
                Producto prod = productoRepo.findById(dto.productoId().longValue())
                        .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
                LoteProducto lote = loteRepo.findById(dto.loteProductoId())
                        .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

                List<Long> almacenesFiltrados = new ArrayList<>();
                Long preBodegaId = inventoryCatalogResolver.getAlmacenPreBodegaProduccionId();


                if (dto.almacenOrigenId() != null) {
                    almacenesFiltrados.add(dto.almacenOrigenId().longValue());
                }

                if (dto.solicitudMovimientoId() != null) {
                    solicitudMovimiento = solicitudMovimientoRepository
                            .findWithDetalles(dto.solicitudMovimientoId())
                            .orElse(null);
                    if (solicitudMovimiento != null
                            && solicitudMovimiento.getAlmacenOrigen() != null
                            && solicitudMovimiento.getAlmacenOrigen().getId() != null) {
                        almacenesFiltrados.add(solicitudMovimiento.getAlmacenOrigen().getId().longValue());
                    }
                }

                if (preBodegaId != null) {
                    almacenesFiltrados.removeIf(id -> Objects.equals(id, preBodegaId));
                }

                if (almacenesFiltrados.isEmpty() && solicitudMovimiento != null) {
                    Set<Long> almacenesDesdeDetalles = new LinkedHashSet<>();
                    if (solicitudMovimiento.getDetalles() != null) {
                        for (SolicitudMovimientoDetalle detalle : solicitudMovimiento.getDetalles()) {
                            if (detalle != null
                                    && detalle.getAlmacenOrigen() != null
                                    && detalle.getAlmacenOrigen().getId() != null) {
                                almacenesDesdeDetalles.add(detalle.getAlmacenOrigen().getId().longValue());
                            }
                        }
                    }
                    if (preBodegaId != null) {
                        almacenesDesdeDetalles.removeIf(id -> Objects.equals(id, preBodegaId));
                    }
                    if (!almacenesDesdeDetalles.isEmpty()) {
                        almacenesFiltrados.addAll(almacenesDesdeDetalles);
                    }
                }

                if (!almacenesFiltrados.isEmpty()) {
                    almacenesFiltrados = new ArrayList<>(new LinkedHashSet<>(almacenesFiltrados));
                }

                log.debug("MOV-CONTROLLER stock pre-check: solicitudId={} productoId={} loteId={} cant={} preBodegaId={} almacenesFiltrados={}",
                        dto.solicitudMovimientoId(), dto.productoId(), dto.loteProductoId(), dto.cantidad(),
                        preBodegaId, almacenesFiltrados);


                if (almacenesFiltrados.isEmpty() && dto.solicitudMovimientoId() == null) {
                    return ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "No hay suficiente stock disponible"));
                }

                BigDecimal cant = dto.cantidad();
                Long productoId = prod.getId().longValue();
                BigDecimal stockProd = stockQueryService
                        .obtenerStockDisponible(List.of(productoId), almacenesFiltrados)
                        .getOrDefault(productoId, BigDecimal.ZERO);
                BigDecimal stockActualLote = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
                BigDecimal stockReservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
                BigDecimal stockDisponibleLote = stockActualLote.subtract(stockReservado);

                BigDecimal stockDisponibleNoNegativo = stockDisponibleLote.compareTo(BigDecimal.ZERO) < 0
                        ? BigDecimal.ZERO
                        : stockDisponibleLote;

                BigDecimal reservaPendiente = BigDecimal.ZERO;
                boolean solicitudConReserva = false;

                if (solicitudMovimiento != null) {
                    reservaPendiente = calcularReservaPendiente(solicitudMovimiento, lote);
                    boolean estadoPermiteReserva = solicitudMovimiento.getEstado() == EstadoSolicitudMovimiento.AUTORIZADA
                            || solicitudMovimiento.getEstado() == EstadoSolicitudMovimiento.PARCIAL
                            || solicitudMovimiento.getEstado() == EstadoSolicitudMovimiento.RESERVADA;
                    boolean reservaDisponible = reservaPendiente.compareTo(BigDecimal.ZERO) > 0;
                    solicitudConReserva = estadoPermiteReserva || reservaDisponible;

                    if (solicitudConReserva) {
                        BigDecimal reservadoPositivo = stockReservado.compareTo(BigDecimal.ZERO) > 0
                                ? stockReservado
                                : BigDecimal.ZERO;
                        if (reservaPendiente.compareTo(reservadoPositivo) > 0) {
                            reservaPendiente = reservadoPositivo;
                        }
                    } else {
                        reservaPendiente = BigDecimal.ZERO;
                    }
                }

                BigDecimal disponibleConReserva = stockDisponibleNoNegativo.add(reservaPendiente);
                BigDecimal stockProductoConReserva = stockProd.add(reservaPendiente);

                BigDecimal stockProductoEvaluado = solicitudConReserva ? stockProductoConReserva : stockProd;
                BigDecimal stockLoteEvaluado = solicitudConReserva ? disponibleConReserva : stockDisponibleNoNegativo;

                log.debug("MOV-CONTROLLER stock eval: solicitudId={} estadoSol={} reservaPendiente={} stockProd={} stockLote={} stockReservado={} dispLote={} dispConReserva={} prodEval={} loteEval={} cant={}",
                        dto.solicitudMovimientoId(),
                        solicitudMovimiento != null ? solicitudMovimiento.getEstado() : null,
                        reservaPendiente, stockProd, stockActualLote, stockReservado, stockDisponibleNoNegativo,
                        disponibleConReserva, stockProductoEvaluado, stockLoteEvaluado, cant);


                boolean stockLoteInsuficiente = stockLoteEvaluado.compareTo(cant) < 0;

                if (solicitudConReserva) {
                    // Cuando consumimos una reserva, validamos solo por lote (+reserva).
                    if (stockLoteInsuficiente) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("message", "No hay suficiente stock disponible"));
                    }
                } else {
                    // Sin solicitud/reserva, mantenemos la doble validación.
                    boolean stockProductoInsuficiente = stockProductoEvaluado.compareTo(cant) < 0;
                    if (stockProductoInsuficiente || stockLoteInsuficiente) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("message", "No hay suficiente stock disponible"));
                    }
                }
            }

            // 2) Registrar movimiento
            MovimientoInventarioResponseDTO creado = service.registrarMovimiento(dto);
            log.info("Movimiento registrado correctamente: {}", creado.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (NoSuchElementException e) {
            log.warn("Error de entidad no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Estado inválido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (DataIntegrityViolationException e) {
            log.error("Violación de integridad en la base de datos", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Datos inválidos o faltantes"));

        } catch (AuthenticationCredentialsNotFoundException e) {
            log.warn("Acceso no autorizado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            log.error("Error inesperado al registrar movimiento", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "No se pudo registrar el movimiento"));
        }
    }

    @Operation(summary = "Consultar movimientos de inventario con filtros opcionales")
    @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_JEFE_PRODUCCION', 'ROL_SUPER_ADMIN', 'ROL_JEFE_CALIDAD')")
    public ResponseEntity<Page<MovimientoInventarioResponseDTO>> filtrar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) TipoMovimiento tipoMovimiento,
            @RequestParam(required = false) ClasificacionMovimientoInventario clasificacion,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @PageableDefault(size = 10, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (fechaInicio == null || fechaFin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren fechaInicio y fechaFin");
        }
        try {
            LocalDateTime inicio = DateParser.parseStart(fechaInicio);
            LocalDateTime fin = DateParser.parseEnd(fechaFin);
            if (inicio.isAfter(fin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaInicio no puede ser mayor a fechaFin");
            }
            Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaIngreso", "id"), "fechaIngreso");
            Page<MovimientoInventarioResponseDTO> page = service.filtrar(
                    inicio, fin, productoId, almacenId, tipoMovimiento, clasificacion, sanitized
            );
            return ResponseEntity.ok(page);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> consultar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) TipoMovimiento tipoMovimiento,
            @RequestParam(required = false) ClasificacionMovimientoInventario clasificacion,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin
    ) {
        if (fechaInicio == null || fechaFin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren fechaInicio y fechaFin");
        }
        try {
            LocalDateTime inicio = DateParser.parseStart(fechaInicio);
            LocalDateTime fin = DateParser.parseEnd(fechaFin);
            if (inicio.isAfter(fin)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaInicio no puede ser mayor a fechaFin");
            }
            MovimientoInventarioFiltroDTO filtro = new MovimientoInventarioFiltroDTO(
                    productoId, almacenId, tipoMovimiento, clasificacion, inicio, fin
            );
            List<MovimientoInventarioResponseDTO> lista = service.consultarMovimientos(filtro);
            return ResponseEntity.ok(lista);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<MovimientoInventarioResponseDTO>> listarTodos(
            @PageableDefault(size = 10, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }
        Pageable sanitized = PaginationUtil.sanitize(pageable, List.of("fechaIngreso", "id"), "fechaIngreso");
        Page<MovimientoInventarioResponseDTO> movimientos = service.listarTodos(sanitized);
        return ResponseEntity.ok(movimientos);
    }

    private BigDecimal calcularReservaPendiente(SolicitudMovimiento solicitud, LoteProducto lote) {
        if (solicitud == null || lote == null || lote.getId() == null) {
            return BigDecimal.ZERO;
        }
        List<SolicitudMovimientoDetalle> detalles = solicitud.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Long loteId = lote.getId();
        BigDecimal total = BigDecimal.ZERO;
        for (SolicitudMovimientoDetalle detalle : detalles) {
            if (detalle == null || detalle.getLote() == null || detalle.getLote().getId() == null) {
                continue;
            }
            if (!Objects.equals(detalle.getLote().getId(), loteId)) {
                continue;
            }
            BigDecimal cantidadDetalle = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO);
            BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO);
            BigDecimal pendiente = cantidadDetalle.subtract(atendida);
            if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
                pendiente = BigDecimal.ZERO;
            }
            total = total.add(pendiente);
        }
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return total;
    }

    private MovimientoInventarioDTO normalizarMovimientoDto(MovimientoInventarioDTO dto) {
        if (dto == null) {
            return null;
        }
        String destinoTexto = dto.destinoTexto();
        if (destinoTexto != null) {
            destinoTexto = destinoTexto.trim();
            if (destinoTexto.isBlank()) {
                destinoTexto = null;
            }
        }
        String docReferencia = dto.docReferencia();
        if ((docReferencia == null || docReferencia.isBlank()) && destinoTexto != null) {
            docReferencia = "DESTINO: " + destinoTexto;
        }
        return new MovimientoInventarioDTO(
                dto.id(),
                dto.cantidad(),
                dto.tipoMovimiento(),
                dto.clasificacionMovimientoInventario(),
                docReferencia,
                destinoTexto,
                dto.productoId(),
                dto.loteProductoId(),
                dto.almacenOrigenId(),
                dto.almacenDestinoId(),
                dto.proveedorId(),
                dto.ordenCompraId(),
                dto.motivoMovimientoId(),
                dto.tipoMovimientoDetalleId(),
                dto.solicitudMovimientoId(),
                dto.usuarioId(),
                dto.ordenProduccionId(),
                dto.ordenCompraDetalleId(),
                dto.codigoLote(),
                dto.fechaVencimiento(),
                dto.estadoLote(),
                dto.autoSplit(),
                dto.atenciones()
        );
    }

}

