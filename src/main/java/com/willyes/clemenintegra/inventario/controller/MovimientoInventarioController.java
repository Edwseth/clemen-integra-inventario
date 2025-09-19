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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Objects;

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

    @Operation(summary = "Registrar un movimiento de inventario")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado correctamente")
    @ApiResponse(responseCode = "400", description = "Solicitud malformada o inválida")
    @ApiResponse(responseCode = "404", description = "Producto o lote no encontrado")
    @ApiResponse(responseCode = "409", description = "No hay suficiente stock disponible")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    @PreAuthorize("hasAnyAuthority('ROL_JEFE_ALMACENES', 'ROL_ALMACENISTA', 'ROL_SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody @Valid MovimientoInventarioDTO dto) {
        try {
            int atenciones = dto.atenciones() != null ? dto.atenciones().size() : 0;
            log.debug("MOV-CONTROLLER registrar solicitudId={} atenciones={} tipo={} clasificacion={} producto={} lote={}",
                    dto.solicitudMovimientoId(), atenciones, dto.tipoMovimiento(),
                    dto.clasificacionMovimientoInventario(), dto.productoId(), dto.loteProductoId());

            // 1) Validación de stock para salidas
            var tipo = dto.tipoMovimiento();
            boolean isSalida = tipo == TipoMovimiento.SALIDA || tipo == TipoMovimiento.AJUSTE;
            SolicitudMovimiento solicitudMovimiento = null;

            if (isSalida) {
                Producto prod = productoRepo.findById(dto.productoId().longValue())
                        .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));
                LoteProducto lote = loteRepo.findById(dto.loteProductoId())
                        .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

                if (dto.solicitudMovimientoId() != null) {
                    solicitudMovimiento = solicitudMovimientoRepository
                            .findWithDetalles(dto.solicitudMovimientoId())
                            .orElse(null);
                }

                BigDecimal cant = dto.cantidad();
                BigDecimal stockProd = stockQueryService.obtenerStockDisponible(prod.getId().longValue());
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

                boolean stockProductoInsuficiente = stockProductoEvaluado.compareTo(cant) < 0;
                boolean stockLoteInsuficiente = stockLoteEvaluado.compareTo(cant) < 0;

                if (stockProductoInsuficiente || stockLoteInsuficiente) {
                    return ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "No hay suficiente stock disponible"));
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

}

