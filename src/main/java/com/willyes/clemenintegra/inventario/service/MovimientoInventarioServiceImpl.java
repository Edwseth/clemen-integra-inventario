package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoInventarioServiceImpl.class);
    /** Nombre normalizado del almacén Pre-Bodega Producción */
    private static final String PRE_BODEGA_PRODUCCION_NORMALIZADO =
            java.text.Normalizer.normalize("Pre-Bodega Producción", java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase();
    private static final Set<RolUsuario> ROLES_OPERATIVOS = EnumSet.of(
            RolUsuario.ROL_JEFE_ALMACENES,
            RolUsuario.ROL_ALMACENISTA,
            RolUsuario.ROL_SUPER_ADMIN
    );

    static final record MovimientoLoteDetalle(LoteProducto lote, BigDecimal cantidad) { }

    static final record ParLoteCantidad(Long loteId, BigDecimal cantidad) { }
    private final AlmacenRepository almacenRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraService ordenCompraService;
    private final LoteProductoRepository loteProductoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository repository;
    private final MovimientoInventarioMapper mapper;
    private final UsuarioService usuarioService;
    private final SolicitudMovimientoRepository solicitudMovimientoRepository;
    private final SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final ReservaLoteService reservaLoteService;
    private final ReservaLoteRepository reservaLoteRepository;
    //private final Long motivoSalidaProdId = catalogResolver.getMotivoSalidaProduccionId();
    //private final Long tipoDetSalidaProdId = catalogResolver.getTipoDetalleSalidaProduccionId();


    @Resource
    private final EntityManager entityManager;
    @Value("${app.inventario.prebodega.id}")
    private Integer preBodegaId;
    @Value("${inventory.tipoDetalle.transferenciaId}")
    private Integer tipoDetalleTransferenciaId;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Intento de registrar movimiento sin autenticación válida");
            throw new AuthenticationCredentialsNotFoundException("No se encontró autenticación válida");
        }

        if (dto.tipoMovimiento() == TipoMovimiento.ENTRADA
                && Objects.equals(dto.motivoMovimientoId(), catalogResolver.getMotivoIdEntradaProductoTerminado())
                && dto.ordenProduccionId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID");
        }

        MovimientoInventario movimiento = mapper.toEntity(dto);

        boolean esOpDesdeDto = dto.ordenProduccionId() != null;
        TipoMovimiento tipoMovimiento = dto.tipoMovimiento();
        ClasificacionMovimientoInventario clasificacion = dto.clasificacionMovimientoInventario();
        Long tipoMovimientoDetalleId = dto.tipoMovimientoDetalleId();
        Integer almacenDestinoIdNormalizado = dto.almacenDestinoId();

        // >>> NUEVO: identifica si el cierre de OP está enviando una ENTRADA de PT
        final boolean esEntradaPt = (tipoMovimiento == TipoMovimiento.ENTRADA);

        if (esOpDesdeDto && !esEntradaPt) {
            tipoMovimiento = TipoMovimiento.TRANSFERENCIA;
            clasificacion = ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
            movimiento.setTipoMovimiento(tipoMovimiento);
            movimiento.setClasificacion(clasificacion);
            log.info("OP_NORMALIZED movimiento: tipo={}, clasificacion={}, opId={}, destino={}",
                    tipoMovimiento, clasificacion, dto.ordenProduccionId(), almacenDestinoIdNormalizado);
        }
       // IMPORTANTe: si es ENTRADA de PT, NO tocar tipo/clasificación aquí

         List<AtencionDTO> atenciones = dto.atenciones() != null
                ? dto.atenciones().stream().filter(Objects::nonNull).collect(Collectors.toList())
                : List.of();
        if (!atenciones.isEmpty()) {
            log.debug("MOV-SERVICE atenciones recibidas: {}", atenciones.size());
        }
        boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());

        List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> detalleRespuesta = List.of();

        // 1. Cargar entidades principales
        Producto producto = productoRepository.findById(dto.productoId().longValue())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        Long resolvedTipoDetalleId = esOpDesdeDto
                ? tipoMovimientoDetalleId
                : resolveTipoMovimientoDetalleId(dto, producto);
        TipoMovimientoDetalle tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(resolvedTipoDetalleId)
                .orElseThrow(() -> new NoSuchElementException("Tipo de detalle de movimiento no encontrado"));
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);

        if (requiereSolicitudMovimientoId(tipoMovimientoDetalle) && dto.solicitudMovimientoId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
        }

        boolean salidaPt = isSalidaPt(tipoMovimiento, resolvedTipoDetalleId);

        Long almacenPtId = null;
        Almacen almacenOrigen;
        if (salidaPt) {
            almacenPtId = ensureAlmacenPtId();
            if (dto.almacenOrigenId() != null && !Objects.equals(dto.almacenOrigenId().longValue(), almacenPtId)) {
                log.warn("ALMACEN_ORIGEN_NO_VALIDO_PT dtoOrigenId={} ptId={}", dto.almacenOrigenId(), almacenPtId);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_ORIGEN_NO_VALIDO_PT");
            }
            almacenOrigen = entityManager.getReference(Almacen.class, almacenPtId);
        } else {
            almacenOrigen = dto.almacenOrigenId() != null
                    ? entityManager.getReference(Almacen.class, dto.almacenOrigenId()) : null;
        }

        Almacen almacenDestino = almacenDestinoIdNormalizado != null
                ? entityManager.getReference(Almacen.class, almacenDestinoIdNormalizado.longValue()) : null;

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        OrdenProduccion ordenProduccion = dto.ordenProduccionId() != null
                ? entityManager.getReference(OrdenProduccion.class, dto.ordenProduccionId())
                : null;

        log.debug("MOV-REQ (pre-solicitud) tipo={}, clasificacion={}, prod={}, qty={}, opIdDTO={}",
                tipoMovimiento, clasificacion, dto.productoId(), dto.cantidad(), dto.ordenProduccionId());

        SolicitudMovimiento solicitud = null;
        if (dto.solicitudMovimientoId() != null) {
            solicitud = solicitudMovimientoRepository.findByIdWithLock(dto.solicitudMovimientoId())
                    .orElseGet(() -> solicitudMovimientoRepository.findWithDetalles(dto.solicitudMovimientoId())
                            .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada")));

            Long solicitudProductoId = solicitud.getProducto() != null ? Long.valueOf(solicitud.getProducto().getId()) : null;
            if (!Objects.equals(solicitudProductoId, dto.productoId() != null ? dto.productoId().longValue() : null)) {
                log.warn("MISMATCH_PRODUCTO_ID: esperado={}, recibido={}", solicitudProductoId, dto.productoId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_PRODUCTO_ID");
            }

            if (solicitud.getTipoMovimiento() != dto.tipoMovimiento()) {
            // Comparar contra el tipo YA normalizado, no contra el del DTO
                if (solicitud.getTipoMovimiento() != tipoMovimiento) {
                    // Compatibilidad: solicitudes antiguas de OP pueden venir con SALIDA,
                    // pero el backend normaliza a TRANSFERENCIA_INTERNA_PRODUCCION.
                    boolean solicitudEsSalidaYBackNormalizoATransferencia =
                            ((dto.ordenProduccionId() != null) || (solicitud.getOrdenProduccion() != null))
                                    && solicitud.getTipoMovimiento() == TipoMovimiento.SALIDA
                                    && tipoMovimiento == TipoMovimiento.TRANSFERENCIA;

                    if (solicitudEsSalidaYBackNormalizoATransferencia) {
                        log.info("OP_SOLICITUD_TIPO_COMPATIBILIZADO: solId={}, esperadoEnSolicitud=SALIDA, usado=TRANSFERENCIA",
                                solicitud.getId());
                    } else {
                        log.warn("MISMATCH_TIPO_MOVIMIENTO: esperado={}, recibido={}", solicitud.getTipoMovimiento(), tipoMovimiento);
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_TIPO_MOVIMIENTO");
                    }
                }

            Long solicitudAlmacenOrigenId = solicitud.getAlmacenOrigen() != null ? Long.valueOf(solicitud.getAlmacenOrigen().getId()) : null;
            Long dtoAlmacenOrigenId = salidaPt ? almacenPtId
                    : dto.almacenOrigenId() != null ? dto.almacenOrigenId().longValue() : null;
            if (solicitudAlmacenOrigenId != null && !Objects.equals(solicitudAlmacenOrigenId, dtoAlmacenOrigenId)) {
                log.warn("MISMATCH_ALMACEN_ORIGEN_ID: esperado={}, recibido={}", solicitudAlmacenOrigenId, dto.almacenOrigenId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_ALMACEN_ORIGEN_ID");
            }

            Long solicitudAlmacenDestinoId = solicitud.getAlmacenDestino() != null ? Long.valueOf(solicitud.getAlmacenDestino().getId()) : null;
            Long dtoAlmacenDestinoId = almacenDestinoIdNormalizado != null
                    ? almacenDestinoIdNormalizado.longValue() : null;
            if (!Objects.equals(solicitudAlmacenDestinoId, dtoAlmacenDestinoId)) {
                log.info("INFO_ALMACEN_DESTINO_IGNORADO: esperadoEnSolicitud={}, recibidoDTO={}, se asignará en backend",
                        solicitudAlmacenDestinoId, dtoAlmacenDestinoId);
            }

            // NOTA: Lote y Cantidad NO generan error si no coinciden
            Long solicitudLoteId = solicitud.getLote() != null ? solicitud.getLote().getId() : null;
            if (!Objects.equals(solicitudLoteId, dto.loteProductoId())) {
                log.info("INFO: Lote distinto al solicitado (esperado={}, recibido={})",
                        solicitudLoteId, dto.loteProductoId());
            }

            if (dto.cantidad() != null && solicitud.getCantidad().compareTo(dto.cantidad()) != 0) {
                log.info("INFO: Cantidad distinta a la solicitada (esperado={}, recibido={})",
                        solicitud.getCantidad(), dto.cantidad());
            }

            // Si la solicitud está vinculada a una orden de producción y el DTO no la
            // proporciona explícitamente, se usa la de la solicitud. Esto garantiza que los
            // movimientos ejecutados a partir de una solicitud queden asociados a la orden
            // correspondiente para el cálculo de insumos consumidos.
            if (ordenProduccion == null && solicitud.getOrdenProduccion() != null) {
                ordenProduccion = solicitud.getOrdenProduccion();
            }

            final Long userActualId = usuario.getId();
            Usuario responsable = solicitud.getUsuarioResponsable();
            Long responsableId = responsable != null ? responsable.getId() : null;
            final boolean esJefeAlmacenes = usuario.getRol() == RolUsuario.ROL_JEFE_ALMACENES;
            final boolean tieneRolPrivilegiado = esJefeAlmacenes || usuario.getRol() == RolUsuario.ROL_SUPER_ADMIN;
            final boolean tienePermisoOperativo = ROLES_OPERATIVOS.contains(usuario.getRol());
            Long responsableIdDesdeDto = dto.usuarioId();
            boolean responsableActualizado = false;
            String eventoResponsable = null;

            if (responsableIdDesdeDto != null && !Objects.equals(responsableId, responsableIdDesdeDto)) {
                if (!Objects.equals(responsableIdDesdeDto, userActualId) && !tieneRolPrivilegiado) {
                    log.warn("RESPONSABLE_DTO_NO_AUTORIZADO: solId={}, responsableActual={}, responsableDto={}, userActual={}",
                            solicitud.getId(), responsableId, responsableIdDesdeDto, userActualId);
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USUARIO_NO_AUTORIZADO");
                }

                if (Objects.equals(responsableIdDesdeDto, userActualId)) {
                    solicitud.setUsuarioResponsable(usuario);
                } else {
                    solicitud.setUsuarioResponsable(entityManager.getReference(Usuario.class, responsableIdDesdeDto));
                }
                responsableId = responsableIdDesdeDto;
                responsableActualizado = true;
                eventoResponsable = "RESPONSABLE_ASIGNADO_DESDE_DTO";
                log.info("RESPONSABLE_ASIGNADO_DESDE_DTO: solId={}, responsableId={}, userActual={}",
                        solicitud.getId(), responsableId, userActualId);
            }

            // 1) Ya resuelta → 409
            if (solicitud.getEstado() == EstadoSolicitudMovimiento.EJECUTADA
                    || solicitud.getEstado() == EstadoSolicitudMovimiento.ATENDIDA
                    || solicitud.getEstado() == EstadoSolicitudMovimiento.CANCELADA) {
                log.warn("SOLICITUD_RESUELTA: solId={}, estado={}, responsableId={}, userActual={}",
                        solicitud.getId(), solicitud.getEstado(), responsableId, userActualId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "SOLICITUD_RESUELTA");
            }

            // 2) Si es reserva, saltar validaciones de aprobación
            if (solicitud.getEstado() != EstadoSolicitudMovimiento.RESERVADA) {
                if (solicitud.getEstado() == EstadoSolicitudMovimiento.PENDIENTE && tieneRolPrivilegiado) {
                    solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
                    log.info("SOLICITUD_AUTO_AUTORIZADA: solId={}, userActual={}",
                            solicitud.getId(), userActualId);
                } else if (solicitud.getEstado() != EstadoSolicitudMovimiento.AUTORIZADA
                        && solicitud.getEstado() != EstadoSolicitudMovimiento.PARCIAL) {
                    log.warn("ESTADO_NO_APROBADO: solId={}, estado={}, responsableId={}, userActual={}",
                            solicitud.getId(), solicitud.getEstado(), responsableId, userActualId);
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "ESTADO_NO_APROBADO: la solicitud aún no está autorizada"
                    );
                }

                // 3) Responsable requerido → 422 o asignación automática
                if (responsableId == null) {
                    if (tienePermisoOperativo) {
                        solicitud.setUsuarioResponsable(usuario);
                        responsableId = userActualId;
                        responsableActualizado = true;
                        eventoResponsable = "RESPONSABLE_AUTOASIGNADO";
                        log.info("RESPONSABLE_AUTOASIGNADO: solId={}, responsableId={}, userActual={}",
                                solicitud.getId(), responsableId, userActualId);
                    } else {
                        log.warn("RESPONSABLE_REQUERIDO: solId={}, responsableId={}, userActual={}",
                                solicitud.getId(), null, userActualId);
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESPONSABLE_REQUERIDO");
                    }
                }

                if (responsableActualizado) {
                    solicitudMovimientoRepository.saveAndFlush(solicitud);
                    String eventoLog = eventoResponsable != null ? eventoResponsable : "RESPONSABLE_ACTUALIZADO";
                    log.info("{}_PERSISTIDO: solId={}, responsableId={}, userActual={}",
                            eventoLog, solicitud.getId(), responsableId, userActualId);
                }

                // 4) Usuario debe ser el responsable o tener rol privilegiado → 403
                if (!responsableId.equals(userActualId) && !tieneRolPrivilegiado) {
                    log.warn("USUARIO_NO_AUTORIZADO: solId={}, responsableId={}, userActual={}",
                            solicitud.getId(), responsableId, userActualId);
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USUARIO_NO_AUTORIZADO");
                }
            }
        }

        if (dto.almacenDestinoId() == null && solicitud != null && solicitud.getAlmacenDestino() != null) {
            almacenDestino = solicitud.getAlmacenDestino();
        }

            // === OP OVERRIDES (solo para TRASLADO DE INSUMOS a Pre-Bodega) ===
            boolean esOP = esOpDesdeDto || (solicitud != null && solicitud.getOrdenProduccion() != null);

            // Cargar motivo si viene en el DTO (para decidir reglas)
            MotivoMovimiento motivoDesdeDto = null;
            if (dto.motivoMovimientoId() != null) {
                motivoDesdeDto = motivoMovimientoRepository.findById(dto.motivoMovimientoId())
                        .orElse(null);
            }

            // Heurística mínima: solo forzar cuando sea el traslado interno de INSUMOS a Pre-Bodega
            boolean esEntradaPorProduccion = (dto.tipoMovimiento() == TipoMovimiento.ENTRADA)
                    || (motivoDesdeDto != null
                    && motivoDesdeDto.getMotivo() == ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO);

            boolean esSalidaProduccion = (dto.tipoMovimiento() == TipoMovimiento.SALIDA)
                    || (motivoDesdeDto != null
                    && motivoDesdeDto.getMotivo() == ClasificacionMovimientoInventario.SALIDA_PRODUCCION);

            boolean esTrasladoInsumosOP =
                    esOP
                            && !esEntradaPorProduccion
                            && !esSalidaProduccion
                            && dto.tipoMovimiento() == TipoMovimiento.TRANSFERENCIA;

            if (esTrasladoInsumosOP && preBodegaId != null) {
                // 1) Forzar destino Pre-Bodega
                if (almacenDestino == null || !Objects.equals(almacenDestino.getId(), preBodegaId.longValue())) {
                    log.info("OP_DESTINO_FORZADO: destinoAnterior={} -> PreBodega({})",
                            (almacenDestino != null ? almacenDestino.getId() : null), preBodegaId);
                }
                almacenDestino = entityManager.getReference(Almacen.class, preBodegaId.longValue());

                // 2) Normalizar tipo/detalle/clasificación a TRASLADO INTERNO PRODUCCIÓN
                if (dto.tipoMovimiento() != TipoMovimiento.TRANSFERENCIA) {
                    tipoMovimiento = TipoMovimiento.TRANSFERENCIA;
                    movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
                }
                if (tipoDetalleTransferenciaId != null
                        && (tipoMovimientoDetalle == null
                        || !Objects.equals(tipoMovimientoDetalle.getId(), tipoDetalleTransferenciaId.longValue()))) {
                    tipoMovimientoDetalle = tipoMovimientoDetalleRepository
                            .findById(tipoDetalleTransferenciaId.longValue())
                            .orElse(tipoMovimientoDetalle);
                }
                clasificacion = ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
                movimiento.setClasificacion(clasificacion);

                log.info("OP_NORMALIZED traslado de insumos: tipo={}, clasificacion={}, opId={}, destino={}",
                        tipoMovimiento, clasificacion, dto.ordenProduccionId(), preBodegaId);
            }
            // === /OP OVERRIDES ===

            // === VALIDACIONES DE CONSISTENCIA (después de normalizar y cargar solicitud) ===
            log.debug("MOV-REQ (post-normalizacion) tipo={}, clasificacion={}, prod={}, qty={}, opIdDTO={}, esOP={}",
                    tipoMovimiento, clasificacion, dto.productoId(), dto.cantidad(), dto.ordenProduccionId(), esOP);
            if (clasificacion == ClasificacionMovimientoInventario.SALIDA_PRODUCCION
                    && tipoMovimiento != TipoMovimiento.SALIDA) {
                log.warn("INCONSISTENT_MOVEMENT: tipo={}, clasificacion={}, opIdDTO={}, esOP={}",
                        tipoMovimiento, clasificacion, dto.ordenProduccionId(), esOP);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "INCONSISTENT_MOVEMENT: SALIDA_PRODUCCION requiere tipoMovimiento=SALIDA");
            }
            if (clasificacion == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION
                    && tipoMovimiento != TipoMovimiento.TRANSFERENCIA) {
                log.warn("INCONSISTENT_MOVEMENT: tipo={}, clasificacion={}, opIdDTO={}, esOP={}",
                        tipoMovimiento, clasificacion, dto.ordenProduccionId(), esOP);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "INCONSISTENT_MOVEMENT: TRANSFERENCIA_INTERNA_PRODUCCION requiere tipoMovimiento=TRANSFERENCIA");
            }

        }
        // === /OP OVERRIDES ===

        // Detección automática de devolución interna
        boolean devolucionInterna = false;
        if (tipoMovimiento == TipoMovimiento.TRANSFERENCIA && almacenOrigen != null && almacenDestino != null) {
            devolucionInterna = esDevolucionInterna(producto, almacenOrigen, almacenDestino);
            if (devolucionInterna) {
                tipoMovimiento = TipoMovimiento.DEVOLUCION;
                movimiento.setTipoMovimiento(TipoMovimiento.DEVOLUCION);
                log.debug("Movimiento detectado como devolución interna");
            }
        }

        validarParametros(tipoMovimiento, almacenOrigen, almacenDestino);

        MotivoMovimiento motivoMovimiento = null;
        if (dto.motivoMovimientoId() != null) {
            motivoMovimiento = motivoMovimientoRepository.findById(dto.motivoMovimientoId())
                    .orElseThrow(() -> new NoSuchElementException("Motivo no encontrado"));
        }

        OrdenCompra orden = null;
        if (tipoMovimiento == TipoMovimiento.RECEPCION
                && motivoMovimiento != null
                && motivoMovimiento.getMotivo() == ClasificacionMovimientoInventario.RECEPCION_COMPRA) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Se requiere una orden de compra para la recepción de compra");
            }
            orden = ordenCompraRepository.findById(dto.ordenCompraId().longValue())
                    .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada"));
            if (orden.getEstado() == EstadoOrdenCompra.CERRADA
                    || orden.getEstado() == EstadoOrdenCompra.CANCELADA
                    || orden.getEstado() == EstadoOrdenCompra.RECHAZADA) {
                throw new IllegalStateException("La orden de compra no se encuentra activa");
            }
            boolean incluido = orden.getDetalles().stream()
                    .anyMatch(d -> d.getProducto() != null &&
                            d.getProducto().getId().equals(producto.getId()));
            if (!incluido) {
                throw new IllegalArgumentException("El producto no pertenece a la orden de compra");
            }
        }

        BigDecimal cantidadSolicitada = dto.cantidad();
        List<MovimientoLoteDetalle> lotesProcesados;

        boolean solicitudConPartidas = solicitud != null
                && solicitud.getDetalles() != null
                && !solicitud.getDetalles().isEmpty();

        if (tipoMovimiento == TipoMovimiento.RECEPCION) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Las recepciones sin Orden de Compra deben usar un lote existente");
            }
            LoteProducto loteRecepcion = crearLoteRecepcion(dto, producto, almacenDestino, usuario,
                    cantidadSolicitada, motivoMovimiento);
            lotesProcesados = List.of(new MovimientoLoteDetalle(loteRecepcion, cantidadSolicitada));

        } else if (salidaPt) {
            lotesProcesados = procesarSalidaPt(dto, producto, cantidadSolicitada,
                    almacenPtId, atenciones, autoSplitSolicitado);

        } else if (solicitudConPartidas) {
            // ⬅️ NUEVO: aprobar por partidas (por cada lote del detalle)
            lotesProcesados = procesarMovimientoPorPartidas(
                    solicitud,
                    producto,
                    almacenDestino,
                    tipoMovimiento,
                    devolucionInterna
            );

        } else {
            // Comportamiento anterior (un solo lote desde DTO)
            lotesProcesados = procesarMovimientoConLoteExistente(
                    dto, tipoMovimiento, almacenOrigen, almacenDestino,
                    producto, cantidadSolicitada, devolucionInterna, solicitud
            );
        }

        if (lotesProcesados == null || lotesProcesados.isEmpty()) {
            throw new IllegalStateException("No se generaron lotes para el movimiento");
        }

        MovimientoLoteDetalle principal = lotesProcesados.get(0);

        OrdenCompraDetalle ordenCompraDetalle = actualizarOrdenCompraDetalle(dto, cantidadSolicitada);
        if (orden != null) {
            ordenCompraService.evaluarYActualizarEstado(orden);
        }

        // El stock disponible se deriva de los lotes, por lo que no se actualiza el producto directamente

        // 6. Asociar entidades al movimiento
        movimiento.setProducto(producto);
        movimiento.setLote(principal.lote());
        movimiento.setCantidad(principal.cantidad());
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setOrdenProduccion(ordenProduccion);
        movimiento.setProveedor(dto.proveedorId() != null
                ? entityManager.getReference(Proveedor.class, dto.proveedorId()) : null);
        movimiento.setOrdenCompra(dto.ordenCompraId() != null
                ? entityManager.getReference(OrdenCompra.class, dto.ordenCompraId()) : null);
        movimiento.setOrdenCompraDetalle(ordenCompraDetalle);
        movimiento.setMotivoMovimiento(dto.motivoMovimientoId() != null
                ? entityManager.getReference(MotivoMovimiento.class, dto.motivoMovimientoId()) : null);
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);
        movimiento.setRegistradoPor(usuario);
        if (solicitud != null) {
            movimiento.setSolicitudMovimiento(solicitud);
        }

        MovimientoInventario guardado = repository.save(movimiento);

        if (solicitud != null) {
            detalleRespuesta = solicitud.getDetalles().stream()
                    .map(d -> {
                        BigDecimal atendidaBD = d.getCantidadAtendida() != null
                                ? d.getCantidadAtendida()
                                : BigDecimal.ZERO;

                        boolean atendida = atendidaBD.compareTo(d.getCantidad()) >= 0
                                // Si tu enum tiene ATENDIDA/ATENDIDO, ajusta el literal:
                                || d.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO;

                        return MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO.builder()
                                .detalleId(d.getId())
                                .loteId(d.getLote() != null ? d.getLote().getId() : null)
                                .codigoLote(d.getLote() != null ? d.getLote().getCodigoLote() : null)
                                .atendida(atendida)
                                .cantidadAtendida(atendidaBD)
                                .cantidadSolicitada(d.getCantidad())
                                .estadoDetalle(d.getEstado())
                                .build();
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        if (lotesProcesados.size() > 1) {
            List<MovimientoInventario> adicionales = new ArrayList<>();
            for (int i = 1; i < lotesProcesados.size(); i++) {
                MovimientoLoteDetalle detalle = lotesProcesados.get(i);
                adicionales.add(duplicarMovimientoBase(movimiento, detalle.lote(), detalle.cantidad()));
            }
            if (!adicionales.isEmpty()) {
                repository.saveAll(adicionales);
            }
        }

        MovimientoInventarioResponseDTO respuesta = mapper.safeToResponseDTO(guardado);
        if (solicitud != null) {
            respuesta.setSolicitudId(solicitud.getId());
            respuesta.setEstadoSolicitud(solicitud.getEstado());
            List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> detalles =
                    detalleRespuesta == null ? List.of() : List.copyOf(detalleRespuesta);
            respuesta.setDetallesSolicitud(detalles);
        }
        return respuesta;
    }

    private List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> atenderSolicitudMovimiento(
            MovimientoInventarioDTO dto,
            SolicitudMovimiento solicitud
    ) {
        entityManager.lock(solicitud, LockModeType.PESSIMISTIC_WRITE);

        List<AtencionDTO> atenciones = obtenerAtencionesParaSolicitud(dto, solicitud);
        if (atenciones.isEmpty()) {
            actualizarEstadoSolicitud(solicitud);
            solicitudMovimientoRepository.saveAndFlush(solicitud);
            return construirRespuestaSolicitud(solicitud);
        }

        for (AtencionDTO atencion : atenciones) {
            BigDecimal cantidad = normalizarCantidad(atencion.getCantidad());
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_CANTIDAD_INVALIDA");
            }

            Long loteId = atencion.getLoteId();
            if (loteId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_LOTE_REQUERIDO");
            }

            LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

            SolicitudMovimientoDetalle detalle = obtenerDetalleParaAtencion(solicitud, atencion);
            if (detalle != null) {
                BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO)
                        .setScale(6, RoundingMode.HALF_UP);
                BigDecimal atendidaPrev = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO)
                        .setScale(6, RoundingMode.HALF_UP);
                BigDecimal pendienteDetalle = solicitada.subtract(atendidaPrev);
                if (pendienteDetalle.compareTo(BigDecimal.ZERO) < 0) {
                    pendienteDetalle = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
                }
                if (pendienteDetalle.compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("IDEMP-DETALLE sin pendiente, se omite consumo detalleId={} solicitudId={} loteId={}",
                            detalle.getId(), solicitud.getId(), loteId);
                    continue;
                }
                if (cantidad.compareTo(pendienteDetalle) > 0) {
                    log.warn("ATENCION_CANTIDAD_EXCEDE_PENDIENTE detalleId={} pendiente={} aprobado={}",
                            detalle.getId(), pendienteDetalle, cantidad);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "ATENCION_CANTIDAD_EXCEDE_PENDIENTE");
                }
            }

            reservaLoteService.consumirReserva(solicitud, detalle, lote, cantidad);

            BigDecimal stockAntes = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
            BigDecimal reservadoAntes = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
            log.debug("VAL-ACTUALIZA antes actualizarStockLote loteId={} stockAntes={} reservadoAntes={} req={}",
                    lote.getId(), stockAntes, reservadoAntes, cantidad);

            actualizarStockLote(lote, cantidad, lote.getProducto());
            loteProductoRepository.save(lote);

            if (detalle != null) {
                actualizarDetalleSolicitud(detalle, cantidad);
                solicitudMovimientoDetalleRepository.save(detalle);
            }
        }

        actualizarEstadoSolicitud(solicitud);
        solicitudMovimientoRepository.saveAndFlush(solicitud);
        return construirRespuestaSolicitud(solicitud);
    }

    private List<AtencionDTO> obtenerAtencionesParaSolicitud(MovimientoInventarioDTO dto, SolicitudMovimiento solicitud) {
        List<AtencionDTO> atenciones = dto.atenciones() != null
                ? dto.atenciones().stream().filter(Objects::nonNull).collect(Collectors.toList())
                : List.of();
        if (!atenciones.isEmpty()) {
            log.debug("SOLICITUD atenciones recibidas: {}", atenciones.size());
            return atenciones;
        }

        if (dto.solicitudMovimientoId() == null || solicitud == null) {
            return List.of();
        }

        List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles()).orElse(List.of());
        if (detalles.isEmpty()) {
            return List.of();
        }

        Long loteObjetivo = dto.loteProductoId();
        if (loteObjetivo == null && solicitud.getLote() != null) {
            loteObjetivo = solicitud.getLote().getId();
        }

        BigDecimal restanteTotal = dto.cantidad() != null
                ? dto.cantidad().setScale(6, RoundingMode.HALF_UP)
                : null;
        BigDecimal cero = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

        List<AtencionDTO> generadas = new ArrayList<>();
        List<SolicitudMovimientoDetalle> detallesCoincidentes = new ArrayList<>();
        List<SolicitudMovimientoDetalle> detallesRestantes = new ArrayList<>();
        for (SolicitudMovimientoDetalle detalle : detalles) {
            if (detalle == null) {
                continue;
            }

            EstadoSolicitudMovimientoDetalle estadoDetalle = detalle.getEstado();
            if (estadoDetalle != EstadoSolicitudMovimientoDetalle.PENDIENTE
                    && estadoDetalle != EstadoSolicitudMovimientoDetalle.PARCIAL) {
                continue;
            }

            Long detalleLoteId = detalle.getLote() != null ? detalle.getLote().getId() : null;
            if (loteObjetivo != null && Objects.equals(loteObjetivo, detalleLoteId)) {
                detallesCoincidentes.add(detalle);
            } else {
                detallesRestantes.add(detalle);
            }
        }

        restanteTotal = generarAtencionesDesdeDetalles(detallesCoincidentes, dto, loteObjetivo, cero, restanteTotal, generadas);

        boolean debeProcesarRestantes = (restanteTotal == null || restanteTotal.compareTo(cero) > 0)
                && !detallesRestantes.isEmpty();
        if (debeProcesarRestantes) {
            restanteTotal = generarAtencionesDesdeDetalles(detallesRestantes, dto, loteObjetivo, cero, restanteTotal, generadas);
        }

        if (restanteTotal != null && restanteTotal.compareTo(cero) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ATENCION_CANTIDAD_EXCEDE_PENDIENTE");
        }

        if (!generadas.isEmpty()) {
            log.debug("SOLICITUD atenciones generadas automaticamente: {}", generadas.size());
            return generadas;
        }
        return List.of();
    }

    private BigDecimal generarAtencionesDesdeDetalles(
            List<SolicitudMovimientoDetalle> detalles,
            MovimientoInventarioDTO dto,
            Long loteObjetivo,
            BigDecimal cero,
            BigDecimal restanteTotal,
            List<AtencionDTO> generadas
    ) {
        for (SolicitudMovimientoDetalle detalle : detalles) {
            if (restanteTotal != null && restanteTotal.compareTo(cero) <= 0) {
                break;
            }

            BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad())
                    .orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida())
                    .orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal pendiente = solicitada.subtract(atendida);
            if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
                pendiente = BigDecimal.ZERO;
            }
            pendiente = pendiente.setScale(6, RoundingMode.HALF_UP);
            if (pendiente.compareTo(cero) <= 0) {
                continue;
            }

            BigDecimal cantidadAtencion = pendiente;
            if (restanteTotal != null && cantidadAtencion.compareTo(restanteTotal) > 0) {
                cantidadAtencion = restanteTotal;
            }
            cantidadAtencion = cantidadAtencion.setScale(6, RoundingMode.HALF_UP);
            if (cantidadAtencion.compareTo(cero) <= 0) {
                continue;
            }

            Long detalleLoteId = detalle.getLote() != null ? detalle.getLote().getId() : null;

            AtencionDTO generado = new AtencionDTO();
            generado.setDetalleId(detalle.getId());
            generado.setLoteId(detalleLoteId != null ? detalleLoteId : loteObjetivo);
            generado.setCantidad(cantidadAtencion);
            generado.setAlmacenOrigenId(
                    detalle.getAlmacenOrigen() != null
                            ? (detalle.getAlmacenOrigen().getId() != null ? detalle.getAlmacenOrigen().getId().intValue() : null)
                            : (dto.almacenOrigenId() != null ? dto.almacenOrigenId().intValue() : null)
            );

            // --- NORMALIZAR A INTEGER EL DESTINO ---
            Integer destinoAtencion =
                    (detalle.getAlmacenDestino() != null
                            ? (detalle.getAlmacenDestino().getId() != null ? detalle.getAlmacenDestino().getId().intValue() : null)
                            : (dto.almacenDestinoId() != null ? dto.almacenDestinoId().intValue() : null)
                    );

            // Para OP, forzar Pre-Bodega como destino de la atención
            if (dto.ordenProduccionId() != null && preBodegaId != null) {
                destinoAtencion = preBodegaId; // ya es Integer
            }

            generado.setAlmacenDestinoId(destinoAtencion);
            generadas.add(generado);


            if (restanteTotal != null) {
                restanteTotal = restanteTotal.subtract(cantidadAtencion).setScale(6, RoundingMode.HALF_UP);
                if (restanteTotal.compareTo(cero) < 0) {
                    restanteTotal = cero;
                }
            }
        }
        return restanteTotal;
    }

    private BigDecimal normalizarCantidad(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.setScale(6, RoundingMode.HALF_UP);
    }

    private void actualizarStockLote(LoteProducto lote, BigDecimal cantidad, Producto producto) {
        BigDecimal stockActual = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal cantidadStock = Optional.ofNullable(cantidad).orElse(BigDecimal.ZERO);
        BigDecimal nuevoStock = stockActual.subtract(cantidadStock);
        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_LOTE_INSUFICIENTE");
        }

        BigDecimal reservadoPendiente = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        if (lote.getId() != null) {
            reservadoPendiente = Optional.ofNullable(
                            reservaLoteRepository.sumPendienteActivaByLoteId(lote.getId(), EstadoReservaLote.ACTIVA))
                    .orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
        }

        log.debug("VAL-RESERVA loteId={} reservadoPendiente={} stockNuevo={}",
                lote.getId(), reservadoPendiente, nuevoStock);

        int escala = resolverEscalaProducto(producto);
        lote.setStockLote(nuevoStock.setScale(escala, RoundingMode.HALF_UP));
        lote.setStockReservado(reservadoPendiente);

        if (lote.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
            lote.setAgotado(true);
            if (lote.getFechaAgotado() == null) {
                lote.setFechaAgotado(LocalDateTime.now());
            }
        } else {
            lote.setAgotado(false);
            lote.setFechaAgotado(null);
        }
    }

    private int resolverEscalaProducto(Producto producto) {
        return catalogResolver.decimals(producto != null ? producto.getUnidadMedida() : null);
    }

    private SolicitudMovimientoDetalle obtenerDetalleParaAtencion(SolicitudMovimiento solicitud, AtencionDTO atencion) {
        if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            return null;
        }

        if (atencion.getDetalleId() != null) {
            SolicitudMovimientoDetalle detalle = solicitudMovimientoDetalleRepository.findById(atencion.getDetalleId())
                    .orElseThrow(() -> new NoSuchElementException("Detalle de solicitud no encontrado"));
            if (!Objects.equals(detalle.getSolicitudMovimiento().getId(), solicitud.getId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_NO_PERTENECE_SOLICITUD");
            }
            return detalle;
        }

        Collection<EstadoSolicitudMovimientoDetalle> estadosValidos = List.of(
                EstadoSolicitudMovimientoDetalle.PENDIENTE,
                EstadoSolicitudMovimientoDetalle.PARCIAL
        );

        Optional<SolicitudMovimientoDetalle> detalleOpt = solicitudMovimientoDetalleRepository
                .findFirstBySolicitudMovimientoIdAndLoteIdAndEstadoInOrderByIdAsc(
                        solicitud.getId(), atencion.getLoteId(), estadosValidos);

        if (detalleOpt.isEmpty()) {
            if (solicitud.getDetalles() != null && !solicitud.getDetalles().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_NO_COMPATIBLE");
            }
            return null;
        }
        return detalleOpt.get();
    }

    private void actualizarDetalleSolicitud(SolicitudMovimientoDetalle detalle, BigDecimal incremento) {
        BigDecimal atendidoPrevio = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO);
        BigDecimal solicitado = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal nuevoAtendido = atendidoPrevio.add(incremento);
        if (solicitado.compareTo(BigDecimal.ZERO) > 0 && nuevoAtendido.compareTo(solicitado) > 0) {
            nuevoAtendido = solicitado;
        }
        detalle.setCantidadAtendida(nuevoAtendido.setScale(6, RoundingMode.HALF_UP));

        if (solicitado.compareTo(BigDecimal.ZERO) <= 0) {
            detalle.setEstado(nuevoAtendido.compareTo(BigDecimal.ZERO) > 0
                    ? EstadoSolicitudMovimientoDetalle.ATENDIDO
                    : EstadoSolicitudMovimientoDetalle.PENDIENTE);
            return;
        }

        if (nuevoAtendido.compareTo(solicitado) >= 0) {
            detalle.setEstado(EstadoSolicitudMovimientoDetalle.ATENDIDO);
        } else {
            detalle.setEstado(EstadoSolicitudMovimientoDetalle.PARCIAL);
        }
    }

    private void actualizarEstadoSolicitud(SolicitudMovimiento solicitud) {
        if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            return;
        }

        long pendientes = solicitudMovimientoDetalleRepository
                .countBySolicitudMovimientoIdAndEstadoNot(solicitud.getId(), EstadoSolicitudMovimientoDetalle.ATENDIDO);

        if (pendientes == 0) {
            solicitud.setEstado(EstadoSolicitudMovimiento.ATENDIDA);
            solicitud.setFechaResolucion(LocalDateTime.now());
        } else {
            solicitud.setEstado(EstadoSolicitudMovimiento.PARCIAL);
            solicitud.setFechaResolucion(null);
        }
    }

    private List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> construirRespuestaSolicitud(
            SolicitudMovimiento solicitud
    ) {
        if (solicitud.getDetalles() == null) {
            return List.of();
        }

        return solicitud.getDetalles().stream()
                .filter(Objects::nonNull)
                .map(det -> SolicitudDetalleAtencionDTO.builder()
                        .detalleId(det.getId())
                        .atendida(det.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO)
                        .cantidadAtendida(det.getCantidadAtendida())
                        .cantidadSolicitada(det.getCantidad())
                        .estadoDetalle(det.getEstado())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<MovimientoInventarioResponseDTO> listarTodos(Pageable pageable) {
        Sort sort = pageable.getSort().isEmpty()
                ? Sort.by(Sort.Direction.DESC, "fechaIngreso")
                : pageable.getSort();

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<MovimientoInventario> movimientos = repository.findAll(sortedPageable);
        return movimientos.map(mapper::safeToResponseDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MovimientoInventarioResponseDTO> filtrar(
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            Long productoId, Long almacenId,
            TipoMovimiento tipoMovimiento, ClasificacionMovimientoInventario clasificacion,
            Pageable pageable) {
        Page<MovimientoInventario> page = repository.filtrar(
                fechaInicio, fechaFin, productoId, almacenId, tipoMovimiento, clasificacion, pageable);
        return page.map(mapper::safeToResponseDTO);
    }

    @Override
    public List<MovimientoInventarioResponseDTO> consultarMovimientos(MovimientoInventarioFiltroDTO filtro) {
        List<MovimientoInventario> lista = repository.buscarMovimientos(
                filtro.fechaInicio(), filtro.fechaFin(),
                filtro.productoId(),
                filtro.almacenId(),
                filtro.tipoMovimiento(),
                filtro.clasificacion()
        );
        return lista.stream().map(mapper::safeToResponseDTO).toList();
    }

    @Override
    public Workbook generarReporteMovimientosExcel() {
        List<MovimientoInventario> movimientos = repository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Movimientos Inventario");

        DataFormat df = workbook.createDataFormat();
        Map<Integer, CellStyle> estilosPorEscala = new HashMap<>();

        // Cabecera
        String[] encabezados = {
                "ID", "Fecha", "Tipo Movimiento", "Clasificación", "Producto", "SKU", "Cantidad", "Unidad Medida",
                "Lote", "Almacén", "Proveedor", "Orden Compra", "Motivo", "Detalle Tipo Movimiento", "Usuario"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < encabezados.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(encabezados[i]);
        }

        // Cuerpo
        int fila = 1;
        for (MovimientoInventario mov : movimientos) {
            Row row = sheet.createRow(fila++);

            row.createCell(0).setCellValue(mov.getId());
            row.createCell(1).setCellValue(mov.getFechaIngreso() != null ? mov.getFechaIngreso().toString() : "");
            row.createCell(2).setCellValue(mov.getTipoMovimiento().name());
            String clasificacion = "-";
            if (mov.getClasificacion() != null) {
                clasificacion = mov.getClasificacion().name();
            } else if (mov.getMotivoMovimiento() != null && mov.getMotivoMovimiento().getMotivo() != null) {
                clasificacion = mov.getMotivoMovimiento().getMotivo().name();
            }
            row.createCell(3).setCellValue(clasificacion);

            String nombreProducto = mov.getProducto() != null ? mov.getProducto().getNombre() : "";
            String codigoSku = mov.getProducto() != null ? mov.getProducto().getCodigoSku() : "";
            String unidad = (mov.getProducto() != null && mov.getProducto().getUnidadMedida() != null)
                    ? mov.getProducto().getUnidadMedida().getNombre() : "";

            row.createCell(4).setCellValue(nombreProducto);
            row.createCell(5).setCellValue(codigoSku);
            BigDecimal cant = mov.getCantidad();
            Cell cCant = row.createCell(6);
            if (cant != null) {
                int escala = catalogResolver.decimals(mov.getProducto() != null
                        ? mov.getProducto().getUnidadMedida() : null);
                cCant.setCellValue(cant.setScale(escala, RoundingMode.HALF_UP).doubleValue());
                cCant.setCellStyle(obtenerEstiloCantidad(workbook, df, estilosPorEscala, escala));
            } else {
                cCant.setBlank();
            }
            row.createCell(7).setCellValue(unidad);
            row.createCell(8).setCellValue(mov.getLote() != null ? mov.getLote().getCodigoLote() : "");
            String nombreAlmacen = mov.getAlmacenDestino() != null
                    ? mov.getAlmacenDestino().getNombre()
                    : (mov.getAlmacenOrigen() != null ? mov.getAlmacenOrigen().getNombre() : "");
            row.createCell(9).setCellValue(nombreAlmacen);
            row.createCell(10).setCellValue(mov.getProveedor() != null ? mov.getProveedor().getNombre() : "");
            row.createCell(11).setCellValue(mov.getOrdenCompra() != null ? mov.getOrdenCompra().getId().toString() : "");
            row.createCell(12).setCellValue(mov.getMotivoMovimiento() != null ? mov.getMotivoMovimiento().getDescripcion() : "");
            row.createCell(13).setCellValue(mov.getTipoMovimientoDetalle() != null ? mov.getTipoMovimientoDetalle().getDescripcion() : "");
            row.createCell(14).setCellValue(mov.getRegistradoPor() != null ? mov.getRegistradoPor().getNombreCompleto() : "");
        }

        // Autosize columnas
        for (int i = 0; i < encabezados.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    public ByteArrayInputStream exportarMovimientosAExcel(List<MovimientoInventario> movimientos) {
        try (Workbook workbook = new XSSFWorkbook()) {
            DataFormat df = workbook.createDataFormat();
            Map<Integer, CellStyle> estilosPorEscala = new HashMap<>();
            Sheet sheet = workbook.createSheet("Movimientos");
            Row header = sheet.createRow(0);
            String[] columnas = {"ID", "Producto", "Cantidad", "Tipo Movimiento", "Fecha"};
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }

            int rowNum = 1;
            for (MovimientoInventario mov : movimientos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(mov.getId());
                String nombreProducto = mov.getProducto() != null ? mov.getProducto().getNombre() : "";
                row.createCell(1).setCellValue(nombreProducto);
                BigDecimal cant = mov.getCantidad();
                Cell c2 = row.createCell(2);
                if (cant != null) {
                    int escala = catalogResolver.decimals(mov.getProducto() != null
                            ? mov.getProducto().getUnidadMedida() : null);
                    c2.setCellValue(cant.setScale(escala, RoundingMode.HALF_UP).doubleValue());
                    c2.setCellStyle(obtenerEstiloCantidad(workbook, df, estilosPorEscala, escala));
                } else {
                    c2.setBlank();
                }
                row.createCell(3).setCellValue(mov.getTipoMovimiento().name());
                row.createCell(4).setCellValue(mov.getFechaIngreso().toString());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new IllegalStateException("Error generando el archivo Excel", e);
        }
    }

    private CellStyle obtenerEstiloCantidad(Workbook workbook,
                                            DataFormat dataFormat,
                                            Map<Integer, CellStyle> estilosPorEscala,
                                            int escala) {
        int escalaNormalizada = Math.max(0, escala);
        return estilosPorEscala.computeIfAbsent(escalaNormalizada, key -> {
            CellStyle estilo = workbook.createCellStyle();
            String formato = "0";
            if (escalaNormalizada > 0) {
                formato = "0." + "0".repeat(escalaNormalizada);
            }
            estilo.setDataFormat(dataFormat.getFormat(formato));
            return estilo;
        });
    }

    private void validarParametros(TipoMovimiento tipo, Almacen origen, Almacen destino) {
        if (tipo == TipoMovimiento.RECEPCION && destino == null) {
            log.warn("Recepción sin destino: tipo={} origenId={} destinoId={}",
                    tipo, origen != null ? origen.getId() : null, destino != null ? destino.getId() : null);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RECEP_REQUIERE_DESTINO");
        }

        if (tipo == TipoMovimiento.TRANSFERENCIA) {
            if (origen == null || destino == null) {
                log.warn("Transferencia requiere almacenes: origenId={} destinoId={}",
                        origen != null ? origen.getId() : null, destino != null ? destino.getId() : null);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSF_REQUIERE_ALMACENES");
            }
            if (Objects.equals(origen.getId(), destino.getId())) {
                log.warn("Transferencia con origen y destino iguales: almacenId={}", origen.getId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSF_ORIGEN_DESTINO_IGUALES");
            }
        }
    }

    private LoteProducto crearLoteRecepcion(MovimientoInventarioDTO dto, Producto producto,
                                            Almacen destino, Usuario usuario, BigDecimal cantidad,
                                            MotivoMovimiento motivoMovimiento) {
        if (dto.loteProductoId() != null) {
            LoteProducto existente = loteProductoRepository.findById(dto.loteProductoId())
                    .orElseThrow(() -> {
                        log.warn(
                                "crearLoteRecepcion: lote no encontrado loteId={} productoId={} destinoId={} cantidad={}",
                                dto.loteProductoId(), producto.getId(),
                                destino != null ? destino.getId() : null, cantidad);
                        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO");
                    });
            BigDecimal nuevo = Optional.ofNullable(existente.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad);
            existente.setStockLote(nuevo);
            existente.setAlmacen(destino);
            return loteProductoRepository.save(existente);
        }

        if (motivoMovimiento == null ||
                motivoMovimiento.getMotivo() != ClasificacionMovimientoInventario.RECEPCION_COMPRA) {
            log.warn(
                    "crearLoteRecepcion: motivo inválido para crear lote productoId={} destinoId={} cantidad={} motivo={}",
                    producto.getId(), destino != null ? destino.getId() : null, cantidad,
                    motivoMovimiento != null ? motivoMovimiento.getMotivo() : null);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_CREACION_MOTIVO_INVALIDO");
        }

        LoteProducto lote = LoteProducto.builder()
                .codigoLote(dto.codigoLote())
                .fechaFabricacion(LocalDateTime.now())
                .fechaVencimiento(dto.fechaVencimiento())
                .fechaLiberacion(producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO ? LocalDateTime.now() : null)
                .estado(obtenerEstadoInicial(producto))
                .producto(producto)
                .almacen(destino)
                .usuarioLiberador(producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO ? usuario : null)
                .stockLote(cantidad)
                .build();
        return loteProductoRepository.save(lote);
    }


    private List<MovimientoLoteDetalle> procesarSalidaPt(MovimientoInventarioDTO dto,
                                                         Producto producto,
                                                         BigDecimal cantidadSolicitada,
                                                         Long almacenPtId,
                                                         List<AtencionDTO> atenciones,
                                                         boolean autoSplitSolicitado) {
        if (almacenPtId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_FALTANTE");
        }
        EnumSet<EstadoLote> estadosElegibles = EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);
        List<AtencionDTO> atencionesSeguras = atenciones != null
                ? atenciones.stream().filter(Objects::nonNull).collect(Collectors.toList())
                : List.of();
        List<ParLoteCantidad> plan = new ArrayList<>();
        if (autoSplitSolicitado && atencionesSeguras.isEmpty()) {
            Integer productoId = producto.getId();
            if (productoId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_ID_REQUERIDO");
            }
            List<LoteProducto> candidatos = loteProductoRepository.findFefoSalidaPt(
                    productoId.longValue(), almacenPtId, estadosElegibles);
            BigDecimal restante = cantidadSolicitada;
            for (LoteProducto candidato : candidatos) {
                if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal stock = Optional.ofNullable(candidato.getStockLote()).orElse(BigDecimal.ZERO);
                BigDecimal reservado = Optional.ofNullable(candidato.getStockReservado()).orElse(BigDecimal.ZERO);
                BigDecimal disponible = stock.subtract(reservado);
                if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal tomar = disponible.min(restante);
                if (tomar.compareTo(BigDecimal.ZERO) > 0) {
                    plan.add(new ParLoteCantidad(candidato.getId(), tomar));
                    restante = restante.subtract(tomar);
                }
            }
            if (restante.compareTo(BigDecimal.ZERO) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "STOCK_INSUFICIENTE");
            }
        } else {
            if (!atencionesSeguras.isEmpty()) {
                BigDecimal suma = BigDecimal.ZERO;
                for (AtencionDTO atencion : atencionesSeguras) {
                    if (atencion.getLoteId() == null || atencion.getCantidad() == null) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACION_ATENCIONES");
                    }
                    plan.add(new ParLoteCantidad(atencion.getLoteId(), atencion.getCantidad()));
                    suma = suma.add(atencion.getCantidad());
                }
                if (suma.compareTo(cantidadSolicitada) != 0) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACION_ATENCIONES");
                }
            } else if (dto.loteProductoId() != null) {
                plan.add(new ParLoteCantidad(dto.loteProductoId(), cantidadSolicitada));
            } else if (autoSplitSolicitado) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACION_ATENCIONES");
            } else {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ID_REQUERIDO");
            }
        }

        List<MovimientoLoteDetalle> detalles = new ArrayList<>();
        for (ParLoteCantidad par : plan) {
            MovimientoLoteDetalle detalle = consumirLoteSalidaPt(par, producto, almacenPtId, estadosElegibles);
            detalles.add(detalle);
        }

        if (detalles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "STOCK_INSUFICIENTE");
        }
        return detalles;
    }

    private MovimientoLoteDetalle consumirLoteSalidaPt(ParLoteCantidad consumo,
                                                       Producto producto,
                                                       Long almacenPtId,
                                                       EnumSet<EstadoLote> estadosElegibles) {
        if (consumo == null || consumo.loteId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ID_REQUERIDO");
        }
        LoteProducto lote = loteProductoRepository.findByIdForUpdate(consumo.loteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO"));
        if (lote.getProducto() == null || producto.getId() == null
                || !Objects.equals(lote.getProducto().getId(), producto.getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_PRODUCTO_INVALIDO");
        }
        if (lote.getAlmacen() == null || lote.getAlmacen().getId() == null
                || !Objects.equals(lote.getAlmacen().getId().longValue(), almacenPtId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
        }
        if (!estadosElegibles.contains(lote.getEstado())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ESTADO_NO_ELEGIBLE");
        }
        BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal reservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
        BigDecimal disponible = stock.subtract(reservado);
        if (disponible.compareTo(consumo.cantidad()) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "STOCK_INSUFICIENTE");
        }
        actualizarStockLote(lote, consumo.cantidad(), producto);
        LoteProducto actualizado = loteProductoRepository.save(lote);
        return new MovimientoLoteDetalle(actualizado, consumo.cantidad());
    }


    private List<MovimientoLoteDetalle> procesarMovimientoConLoteExistente(MovimientoInventarioDTO dto,
                                                                           TipoMovimiento tipo,
                                                                           Almacen origen,
                                                                           Almacen destino,
                                                                           Producto producto,
                                                                           BigDecimal cantidad,
                                                                           boolean devolucionInterna,
                                                                           SolicitudMovimiento solicitud) {
        if (dto.loteProductoId() == null) {
            log.warn(
                    "procesarMovimientoConLoteExistente: falta loteProductoId tipo={} productoId={} origenId={} destinoId={} cantidad={}",
                    tipo, producto.getId(), origen != null ? origen.getId() : null,
                    destino != null ? destino.getId() : null, cantidad);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ID_REQUERIDO");
        }

        LoteProducto loteOrigen = loteProductoRepository.findByIdForUpdate(dto.loteProductoId())
                .orElseThrow(() -> {
                    log.warn(
                            "procesarMovimientoConLoteExistente: lote no encontrado loteId={} productoId={}",
                            dto.loteProductoId(), producto.getId());
                    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO");
                });

        boolean esPorLote = solicitud != null;
        log.debug("VAL-GATE esPorLote={} solicitudId={} tipo={}", esPorLote,
                solicitud != null ? solicitud.getId() : null, tipo);

        boolean estadoBloqueado = EnumSet.of(EstadoLote.RETENIDO,
                        EstadoLote.RECHAZADO, EstadoLote.VENCIDO)
                .contains(loteOrigen.getEstado())
                || (loteOrigen.getEstado() == EstadoLote.EN_CUARENTENA
                && producto.getCategoriaProducto().getTipo() != TipoCategoria.PRODUCTO_TERMINADO);

        if (estadoBloqueado) {
            log.warn(
                    "procesarMovimientoConLoteExistente: estado de lote inválido loteId={} estado={} productoId={}",
                    loteOrigen.getId(), loteOrigen.getEstado(), producto.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ESTADO_INVALIDO");
        }

        Almacen almacenOrigen = origen != null
                ? origen
                : (dto.almacenOrigenId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenOrigenId())
                : null);

        boolean esDevolucionInternaCalculada =
                dto.tipoMovimiento() == TipoMovimiento.DEVOLUCION
                        && dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION;

        if (!esDevolucionInternaCalculada
                && almacenOrigen != null
                && !loteOrigen.getAlmacen().getId().equals(almacenOrigen.getId())) {
            log.warn("Almacén origen no coincide: loteId={} almacenLoteId={} almacenOrigenId={}",
                    loteOrigen.getId(), loteOrigen.getAlmacen().getId(), almacenOrigen.getId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
        }

        BigDecimal stockActual = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(BigDecimal.ZERO);
        BigDecimal disponible = stockActual.subtract(reservadoActual);
        BigDecimal disponibleNoNegativo = disponible.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : disponible;
        boolean solicitudAutorizadaOParcial = solicitud != null
                && (solicitud.getEstado() == EstadoSolicitudMovimiento.AUTORIZADA
                || solicitud.getEstado() == EstadoSolicitudMovimiento.PARCIAL);
        BigDecimal reservaPendiente = BigDecimal.ZERO;
        if (solicitudAutorizadaOParcial) {
            reservaPendiente = calcularReservaPendiente(solicitud, loteOrigen);
            BigDecimal reservadoPositivo = reservadoActual.compareTo(BigDecimal.ZERO) > 0
                    ? reservadoActual
                    : BigDecimal.ZERO;
            if (reservaPendiente.compareTo(reservadoPositivo) > 0) {
                reservaPendiente = reservadoPositivo;
            }
        }
        BigDecimal disponibleConReserva = solicitudAutorizadaOParcial
                ? disponibleNoNegativo.add(reservaPendiente)
                : disponibleNoNegativo;

        log.debug("VAL-LOTE loteId={} stockLote={} reservadoTotal={} pendienteSolicitud={} disponible={} req={}",
                loteOrigen.getId(), stockActual, reservadoActual, reservaPendiente, disponibleConReserva, cantidad);

        boolean esTransferenciaInternaProduccion = tipo == TipoMovimiento.TRANSFERENCIA
                && dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
        boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());
        boolean requiereAutoSplit = tipo == TipoMovimiento.TRANSFERENCIA
                && esTransferenciaInternaProduccion
                && autoSplitSolicitado
                && cantidad.compareTo(disponibleNoNegativo) > 0;

        if (EnumSet.of(TipoMovimiento.SALIDA, TipoMovimiento.TRANSFERENCIA,
                TipoMovimiento.DEVOLUCION, TipoMovimiento.AJUSTE).contains(tipo)) {
            if (solicitud != null
                    && solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA
                    && !requiereAutoSplit) {
                if (reservadoActual.compareTo(cantidad) < 0) {
                    log.warn("RESERVA_INSUFICIENTE: loteId={} reservado={} solicitado={} productoId={}",
                            loteOrigen.getId(), reservadoActual, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_INSUFICIENTE");
                }
            } else if (!requiereAutoSplit) {
                if (disponibleConReserva.compareTo(cantidad) < 0) {
                    log.warn("Stock insuficiente en lote: loteId={} disponible={} reservaPendiente={} solicitado={} productoId={}",
                            loteOrigen.getId(), disponibleNoNegativo, reservaPendiente, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_STOCK_INSUFICIENTE");
                }
            }
        }

        if (tipo == TipoMovimiento.SALIDA) {
            log.debug("MOV-SALIDA procesando prod={}, qty={}, solicitudId={}, opId={}",
                    dto.productoId(), cantidad, solicitud != null ? solicitud.getId() : null, dto.ordenProduccionId());
            if (solicitud != null) {
                log.debug("MOV-SALIDA delegando ajuste de lote a la atención de solicitud solicitudId={} loteId={}",
                        solicitud.getId(), loteOrigen.getId());
                return List.of(new MovimientoLoteDetalle(loteOrigen, cantidad));
            }
            BigDecimal stockAntes = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO);
            BigDecimal reservadoAntes = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(BigDecimal.ZERO);
            log.debug("VAL-ACTUALIZA antes actualizarStockLote loteId={} stockAntes={} reservadoAntes={} req={}",
                    loteOrigen.getId(), stockAntes, reservadoAntes, cantidad);
            actualizarStockLote(loteOrigen, cantidad, producto);
            LoteProducto actualizado = loteProductoRepository.save(loteOrigen);
            return List.of(new MovimientoLoteDetalle(actualizado, cantidad));
        }

        if (tipo == TipoMovimiento.ENTRADA) {
            BigDecimal nuevo = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad);
            loteOrigen.setStockLote(nuevo);
            if (loteOrigen.isAgotado() && nuevo.compareTo(BigDecimal.ZERO) > 0) {
                loteOrigen.setAgotado(false);
                loteOrigen.setFechaAgotado(null);
            }
            LoteProducto actualizado = loteProductoRepository.save(loteOrigen);
            return List.of(new MovimientoLoteDetalle(actualizado, cantidad));
        }

        if (tipo == TipoMovimiento.TRANSFERENCIA) {
            if (loteOrigen.getEstado() != EstadoLote.DISPONIBLE) {
                log.warn(
                        "Transferencia con lote no disponible: loteId={} estado={} origenId={} destinoId={} productoId={} cantidad={}",
                        loteOrigen.getId(), loteOrigen.getEstado(),
                        origen != null ? origen.getId() : null,
                        destino != null ? destino.getId() : null,
                        producto.getId(), cantidad);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_DISPONIBLE_TRANSFERIR");
            }

            if (!requiereAutoSplit) {
                MovimientoLoteDetalle detalle = ejecutarTransferenciaDesdeLote(loteOrigen, destino, producto, cantidad, solicitud);
                return List.of(detalle);
            }

            if (destino == null) {
                log.warn("AUTO_SPLIT_DESTINO_REQUERIDO: productoId={} loteId={}", producto.getId(), loteOrigen.getId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_REQUERIDO");
            }

            List<MovimientoLoteDetalle> detalles = new ArrayList<>();
            List<ParLoteCantidad> consumidos = new ArrayList<>();

            BigDecimal cantidadInicial = cantidad.min(disponibleNoNegativo);
            if (cantidadInicial.compareTo(BigDecimal.ZERO) > 0) {
                detalles.add(ejecutarTransferenciaDesdeLote(loteOrigen, destino, producto, cantidadInicial, solicitud));
                consumidos.add(new ParLoteCantidad(loteOrigen.getId(), cantidadInicial));
            }

            BigDecimal restante = cantidad.subtract(cantidadInicial);
            if (restante.compareTo(BigDecimal.ZERO) < 0) {
                restante = BigDecimal.ZERO;
            }

            if (restante.compareTo(BigDecimal.ZERO) > 0) {
                Integer almacenOrigenId = almacenOrigen != null
                        ? almacenOrigen.getId()
                        : (loteOrigen.getAlmacen() != null ? loteOrigen.getAlmacen().getId() : dto.almacenOrigenId());
                if (almacenOrigenId == null) {
                    log.warn("AUTO_SPLIT_ALMACEN_ORIGEN_NO_DEFINIDO productoId={} loteId={}",
                            producto.getId(), loteOrigen.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_ORIGEN_REQUERIDO");
                }
                Long productoId = producto.getId() != null ? producto.getId().longValue() : null;
                if (productoId == null) {
                    log.warn("AUTO_SPLIT_PRODUCTO_NO_DEFINIDO loteId={}", loteOrigen.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCTO_ID_REQUERIDO");
                }

                List<ParLoteCantidad> plan = planificarAutoSplit(productoId, almacenOrigenId,
                        loteOrigen.getId(), restante);
                for (ParLoteCantidad par : plan) {
                    LoteProducto loteAdicional = loteProductoRepository.findByIdForUpdate(par.loteId())
                            .orElseThrow(() -> {
                                log.warn("AUTO_SPLIT_LOTE_ADICIONAL_NO_ENCONTRADO loteId={} productoId={}",
                                        par.loteId(), productoId);
                                return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO");
                            });

                    if (loteAdicional.getProducto() == null
                            || !Objects.equals(loteAdicional.getProducto().getId(), producto.getId())) {
                        log.warn("AUTO_SPLIT_PRODUCTO_INCONSISTENTE loteId={} productoEsperado={} productoEncontrado={}",
                                loteAdicional.getId(), producto.getId(),
                                loteAdicional.getProducto() != null ? loteAdicional.getProducto().getId() : null);
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_PRODUCTO_INVALIDO");
                    }
                    if (loteAdicional.getAlmacen() == null
                            || !Objects.equals(loteAdicional.getAlmacen().getId(), almacenOrigenId)) {
                        log.warn("AUTO_SPLIT_ALMACEN_INCONSISTENTE loteId={} almacenEsperado={} almacenEncontrado={}",
                                loteAdicional.getId(), almacenOrigenId,
                                loteAdicional.getAlmacen() != null ? loteAdicional.getAlmacen().getId() : null);
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
                    }

                    MovimientoLoteDetalle parcial = ejecutarTransferenciaDesdeLote(loteAdicional, destino, producto, par.cantidad(), solicitud);
                    detalles.add(parcial);
                    consumidos.add(new ParLoteCantidad(loteAdicional.getId(), par.cantidad()));
                }
            }

            BigDecimal totalTransferido = detalles.stream()
                    .map(MovimientoLoteDetalle::cantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalTransferido.compareTo(cantidad) != 0) {
                log.error("AUTO_SPLIT_TOTAL_INCONSISTENTE esperado={} obtenido={} productoId={} almacenOrigenId={}",
                        cantidad, totalTransferido, producto.getId(),
                        almacenOrigen != null ? almacenOrigen.getId() : null);
                throw new IllegalStateException("AUTO_SPLIT_TOTAL_INCONSISTENTE");
            }

            if (!consumidos.isEmpty()) {
                String lotesLog = consumidos.stream()
                        .map(par -> String.format("{loteId=%d, cantidad=%s}", par.loteId(), par.cantidad()))
                        .collect(Collectors.joining(", "));
                log.info("AUTO_SPLIT_FEFO productoId={} opId={} almacenOrigenId={} lotes=[{}] total={}",
                        producto.getId(), dto.ordenProduccionId(),
                        almacenOrigen != null ? almacenOrigen.getId() : null,
                        lotesLog, cantidad);
            }

            return detalles;
        }

        if (tipo == TipoMovimiento.DEVOLUCION && destino != null) {
            loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
            loteProductoRepository.save(loteOrigen);

            Optional<LoteProducto> destinoExistente = loteProductoRepository
                    .findByCodigoLoteAndProductoIdAndAlmacenId(
                            loteOrigen.getCodigoLote(),
                            producto.getId(),
                            destino.getId());

            LoteProducto loteDestino = destinoExistente.orElseGet(() -> LoteProducto.builder()
                    .producto(producto)
                    .codigoLote(loteOrigen.getCodigoLote())
                    .fechaFabricacion(loteOrigen.getFechaFabricacion())
                    .fechaVencimiento(loteOrigen.getFechaVencimiento())
                    .estado(loteOrigen.getEstado())
                    .almacen(destino)
                    .stockLote(BigDecimal.ZERO)
                    .build());

            BigDecimal nuevoStock = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO).add(cantidad);
            loteDestino.setStockLote(nuevoStock);
            LoteProducto actualizado = loteProductoRepository.save(loteDestino);
            return List.of(new MovimientoLoteDetalle(actualizado, cantidad));
        }

        return List.of(new MovimientoLoteDetalle(loteOrigen, cantidad));
    }

    private MovimientoLoteDetalle ejecutarTransferenciaDesdeLote(LoteProducto loteOrigen,
                                                                 Almacen destino,
                                                                 Producto producto,
                                                                 BigDecimal cantidadTransferir,
                                                                 SolicitudMovimiento solicitud) {
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_REQUERIDO");
        }

        BigDecimal cantidadNormalizada = Optional.ofNullable(cantidadTransferir)
                .map(c -> c.setScale(6, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        if (cantidadNormalizada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_TRANSFERENCIA_INVALIDA");
        }

        BigDecimal stockActual = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO);
        if (stockActual.compareTo(cantidadNormalizada) < 0) {
            log.warn("TRANSFERENCIA_STOCK_INSUFICIENTE loteId={} stockActual={} requerido={}",
                    loteOrigen.getId(), stockActual, cantidadNormalizada);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LOTE_STOCK_INSUFICIENTE");
        }

        LoteProducto loteDestino = ensureDestinoLote(producto, loteOrigen.getCodigoLote(), loteOrigen, destino);

        int escala = resolverEscalaProducto(producto);
        BigDecimal nuevoStockOrigen = stockActual.subtract(cantidadNormalizada)
                .setScale(escala, RoundingMode.HALF_UP);
        loteOrigen.setStockLote(nuevoStockOrigen);

        if (esAtencionReserva(solicitud)) {
            BigDecimal reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(BigDecimal.ZERO);
            BigDecimal decremento = reservadoActual.min(cantidadNormalizada);
            BigDecimal nuevoReservado = reservadoActual.subtract(decremento);
            if (nuevoReservado.compareTo(BigDecimal.ZERO) < 0) {
                nuevoReservado = BigDecimal.ZERO;
            }
            loteOrigen.setStockReservado(nuevoReservado.setScale(6, RoundingMode.HALF_UP));
        }

        recalcularAgotadoSegunDisponibilidad(loteOrigen);
        loteProductoRepository.save(loteOrigen);

        BigDecimal stockDestino = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal nuevoStockDestino = stockDestino.add(cantidadNormalizada)
                .setScale(escala, RoundingMode.HALF_UP);
        loteDestino.setStockLote(nuevoStockDestino);
        if (loteDestino.getStockReservado() == null) {
            loteDestino.setStockReservado(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        }
        if (destino.getCategoria() == TipoCategoria.OBSOLETOS) {
            loteDestino.setEstado(EstadoLote.RECHAZADO);
        } else if (loteDestino.getEstado() == null) {
            loteDestino.setEstado(EstadoLote.DISPONIBLE);
        }

        recalcularAgotadoSegunDisponibilidad(loteDestino);
        LoteProducto guardadoDestino = loteProductoRepository.save(loteDestino);
        BigDecimal cantidadDetalle = cantidadNormalizada.setScale(escala, RoundingMode.HALF_UP);
        return new MovimientoLoteDetalle(guardadoDestino, cantidadDetalle);
    }

    private LoteProducto ensureDestinoLote(
            Producto producto,
            String codigoLote,
            LoteProducto loteOrigen,
            Almacen almacenDestino
    ) {
        Integer prodId = producto.getId();
        Integer destinoId = almacenDestino.getId();

        // 1) Intentar encontrar el lote EXACTO en el almacén destino
        Optional<LoteProducto> exacto = loteProductoRepository
                .findByCodigoLoteAndProductoIdAndAlmacenId(codigoLote, prodId, destinoId);

        if (exacto.isPresent()) {
            return exacto.get();
        }

        // 2) Si NO existe en el destino, lo creamos en destino (clonando metadatos relevantes)
        LoteProducto nuevo = new LoteProducto();
        nuevo.setProducto(producto);
        nuevo.setCodigoLote(codigoLote);
        nuevo.setAlmacen(almacenDestino);
        // Copia metadatos útiles del origen si aplica
        if (loteOrigen != null) {
            nuevo.setEstado(loteOrigen.getEstado());
            nuevo.setFechaFabricacion(loteOrigen.getFechaFabricacion());
            nuevo.setFechaVencimiento(loteOrigen.getFechaVencimiento());
            nuevo.setTemperaturaAlmacenamiento(loteOrigen.getTemperaturaAlmacenamiento());
            // NO copiar stock: se ajustará por el movimiento
            nuevo.setStockLote(BigDecimal.ZERO);
            nuevo.setStockReservado(BigDecimal.ZERO);
        } else {
            nuevo.setStockLote(BigDecimal.ZERO);
            nuevo.setStockReservado(BigDecimal.ZERO);
        }

        LoteProducto guardado = loteProductoRepository.save(nuevo);
        log.info("DESTINO_LOTE_CREADO: code={}, prod={}, destinoId={}, loteId={}",
                codigoLote, prodId, destinoId, guardado.getId());
        return guardado;
    }

    private void recalcularAgotadoSegunDisponibilidad(LoteProducto lote) {
        BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal reservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal disponible = stock.subtract(reservado);
        if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
            lote.setAgotado(true);
            lote.setFechaAgotado(LocalDateTime.now());
        } else {
            lote.setAgotado(false);
            lote.setFechaAgotado(null);
        }
    }

    private BigDecimal calcularReservaPendiente(SolicitudMovimiento solicitud, LoteProducto lote) {
        if (solicitud == null || lote == null || lote.getId() == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        List<SolicitudMovimientoDetalle> detalles = solicitud.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        Long loteId = lote.getId();
        BigDecimal total = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        for (SolicitudMovimientoDetalle detalle : detalles) {
            if (detalle == null) {
                continue;
            }
            LoteProducto detalleLote = detalle.getLote();
            if (detalleLote == null || detalleLote.getId() == null
                    || !Objects.equals(detalleLote.getId(), loteId)) {
                continue;
            }
            BigDecimal cantidadDetalle = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal pendiente = cantidadDetalle.subtract(atendida);
            if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
                pendiente = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            }
            total = total.add(pendiente);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private boolean esAtencionReserva(SolicitudMovimiento solicitud) {
        if (solicitud == null) {
            return false;
        }
        return solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA
                || solicitud.getOrdenProduccion() != null;
    }

    private List<ParLoteCantidad> planificarAutoSplit(Long productoId,
                                                      Integer almacenOrigenId,
                                                      Long loteInicialId,
                                                      BigDecimal requeridoRemanente) {
        BigDecimal objetivo = Optional.ofNullable(requeridoRemanente).orElse(BigDecimal.ZERO);
        if (objetivo.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<LoteProducto> candidatos = loteProductoRepository
                .findByProductoIdAndAlmacenIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
                        productoId,
                        almacenOrigenId,
                        List.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO));

        List<ParLoteCantidad> resultado = new ArrayList<>();
        BigDecimal restante = objetivo;

        for (LoteProducto lote : candidatos) {
            if (Objects.equals(lote.getId(), loteInicialId)) {
                continue;
            }
            if (lote.isAgotado()) {
                continue;
            }
            BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
            BigDecimal reservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
            BigDecimal disponible = stock.subtract(reservado);
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal tomar = disponible.min(restante);
            if (tomar.compareTo(BigDecimal.ZERO) > 0) {
                resultado.add(new ParLoteCantidad(lote.getId(), tomar));
                restante = restante.subtract(tomar);
                if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal disponibleTotal = objetivo.subtract(restante);
            log.warn("AUTO_SPLIT_STOCK_INSUFICIENTE productoId={} almacenOrigenId={} requerido={} disponible={}",
                    productoId, almacenOrigenId, objetivo, disponibleTotal);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "STOCK_INSUFICIENTE_EN_ALMACEN");
        }

        return resultado;
    }

    private Long resolveTipoMovimientoDetalleId(MovimientoInventarioDTO dto, Producto producto) {
        if (dto.tipoMovimientoDetalleId() != null) {
            return dto.tipoMovimientoDetalleId();
        }
        if (dto.tipoMovimiento() != TipoMovimiento.SALIDA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TIPO_MOVIMIENTO_DETALLE_ID_REQUERIDO");
        }
        if (!puedeAutocompletarSalidaPt(dto, producto)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "tipoMovimientoDetalleId es requerido para SALIDAS no PT");
        }
        Long salidaPtId = catalogResolver.getTipoDetalleSalidaPtId();
        if (salidaPtId == null) {
            Long salidaId = catalogResolver.getTipoDetalleSalidaId();
            if (salidaId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_FALTANTE");
            }
            return salidaId;
        }
        return salidaPtId;
    }

    private boolean puedeAutocompletarSalidaPt(MovimientoInventarioDTO dto, Producto producto) {
        if (dto == null || producto == null) {
            return false;
        }
        if (!tieneSenalSalidaPt(dto)) {
            return false;
        }
        boolean productoEsPt = producto.getCategoriaProducto() != null
                && producto.getCategoriaProducto().getTipo() == TipoCategoria.PRODUCTO_TERMINADO;
        Long almacenPtId = catalogResolver.getAlmacenPtId();
        boolean origenCompatible = almacenPtId != null
                && (dto.almacenOrigenId() == null
                || Objects.equals(dto.almacenOrigenId().longValue(), almacenPtId));
        return productoEsPt || origenCompatible;
    }

    private boolean tieneSenalSalidaPt(MovimientoInventarioDTO dto) {
        String doc = dto.docReferencia();
        if (doc != null && !doc.isBlank()) {
            return true;
        }
        String destino = dto.destinoTexto();
        return destino != null && !destino.isBlank();
    }

    private boolean isSalidaPt(TipoMovimiento tipoMovimiento, Long tipoDetalleId) {
        if (tipoMovimiento != TipoMovimiento.SALIDA || tipoDetalleId == null) {
            return false;
        }
        Long salidaPtId = catalogResolver.getTipoDetalleSalidaPtId();
        if (salidaPtId != null) {
            return Objects.equals(tipoDetalleId, salidaPtId);
        }
        Long salidaId = catalogResolver.getTipoDetalleSalidaId();
        return salidaId != null && Objects.equals(tipoDetalleId, salidaId);
    }

    private Long ensureAlmacenPtId() {
        Long almacenPtId = catalogResolver.getAlmacenPtId();
        if (almacenPtId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_FALTANTE");
        }
        return almacenPtId;
    }

    private MovimientoInventario duplicarMovimientoBase(MovimientoInventario base,
                                                        LoteProducto lote,
                                                        BigDecimal cantidad) {
        MovimientoInventario copia = new MovimientoInventario();
        copia.setCantidad(cantidad);
        copia.setTipoMovimiento(base.getTipoMovimiento());
        copia.setClasificacion(base.getClasificacion());
        copia.setDocReferencia(base.getDocReferencia());
        copia.setRegistradoPor(base.getRegistradoPor());
        copia.setProducto(base.getProducto());
        copia.setLote(lote);
        copia.setAlmacenOrigen(base.getAlmacenOrigen());
        copia.setAlmacenDestino(base.getAlmacenDestino());
        copia.setProveedor(base.getProveedor());
        copia.setOrdenCompra(base.getOrdenCompra());
        copia.setMotivoMovimiento(base.getMotivoMovimiento());
        copia.setOrdenProduccion(base.getOrdenProduccion());
        copia.setTipoMovimientoDetalle(base.getTipoMovimientoDetalle());
        copia.setOrdenCompraDetalle(base.getOrdenCompraDetalle());
        copia.setSolicitudMovimiento(base.getSolicitudMovimiento());
        return copia;
    }

    private OrdenCompraDetalle actualizarOrdenCompraDetalle(MovimientoInventarioDTO dto, BigDecimal cantidad) {
        if (dto.ordenCompraDetalleId() == null) {
            return null;
        }
        OrdenCompraDetalle detalle = entityManager.getReference(OrdenCompraDetalle.class, dto.ordenCompraDetalleId());
        BigDecimal recibida = Optional.ofNullable(detalle.getCantidadRecibida()).orElse(BigDecimal.ZERO);
        BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal nuevaCantidad = recibida.add(cantidad);
        if (nuevaCantidad.compareTo(solicitada) > 0) {
            log.warn(
                    "Cantidad recibida excede solicitada: ocDetalleId={} productoId={} solicitada={} nueva={} movCantidad={}",
                    dto.ordenCompraDetalleId(), dto.productoId(), solicitada, nuevaCantidad, cantidad);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ORDEN_CANTIDAD_EXCEDIDA");
        }
        detalle.setCantidadRecibida(nuevaCantidad);
        return entityManager.merge(detalle);
    }

    private EstadoLote obtenerEstadoInicial(Producto producto) {
        return producto.getTipoAnalisisCalidad() == TipoAnalisisCalidad.NINGUNO
                ? EstadoLote.DISPONIBLE
                : EstadoLote.EN_CUARENTENA;
    }

    /**
     * Detecta si un movimiento marcado como transferencia corresponde en realidad
     * a una devolución interna entre bodegas.
     *
     * @param producto       Producto en movimiento
     * @param almacenOrigen  almacén desde donde se mueve
     * @param almacenDestino almacén hacia donde se mueve
     * @return {@code true} si se cumplen las reglas de devolución interna
     */
    private boolean esDevolucionInterna(Producto producto, Almacen almacenOrigen, Almacen almacenDestino) {
        if (producto == null || producto.getCategoriaProducto() == null
                || almacenOrigen == null || almacenDestino == null) {
            return false;
        }

        var tipo = producto.getCategoriaProducto().getTipo();
        boolean categoriaPermitida = tipo == TipoCategoria.MATERIA_PRIMA
                || tipo == TipoCategoria.MATERIAL_EMPAQUE;

        String nombreOrigen = normalizar(almacenOrigen.getNombre());
        String nombreDestino = normalizar(almacenDestino.getNombre());

        boolean origenPreBodega = PRE_BODEGA_PRODUCCION_NORMALIZADO.equals(nombreOrigen);
        boolean destinoDiferente = !PRE_BODEGA_PRODUCCION_NORMALIZADO.equals(nombreDestino);

        return categoriaPermitida && origenPreBodega && destinoDiferente;
    }

    /**
     * Normaliza un nombre de almacén ignorando mayúsculas y acentos.
     */
    private String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }
        String nfd = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").toLowerCase();
    }

    private boolean requiereSolicitudMovimientoId(TipoMovimientoDetalle tipoMovimientoDetalle) {
        if (tipoMovimientoDetalle == null) {
            return false;
        }
        String descripcion = tipoMovimientoDetalle.getDescripcion();
        if (descripcion == null || descripcion.isBlank()) {
            return false;
        }
        String normalized = java.text.Normalizer.normalize(descripcion, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase();
        return normalized.contains("SOLICITUD") || normalized.contains("RESERVA");
    }

    private static final BigDecimal ZERO = new BigDecimal("0");

    @Transactional
    private List<MovimientoLoteDetalle> procesarMovimientoPorPartidas(
            SolicitudMovimiento solicitud,
            Producto producto,
            Almacen almacenDestino,
            TipoMovimiento tipoMovimiento,
            boolean devolucionInterna
    ) {
        List<MovimientoLoteDetalle> result = new ArrayList<>();

        // Solo procesamos partidas PENDIENTE o PARCIAL
        for (SolicitudMovimientoDetalle det : solicitud.getDetalles()) {
            if (det.getEstado() != EstadoSolicitudMovimientoDetalle.PENDIENTE
                    && det.getEstado() != EstadoSolicitudMovimientoDetalle.PARCIAL) {
                continue;
            }

            BigDecimal atendida = Optional.ofNullable(det.getCantidadAtendida()).orElse(ZERO);
            BigDecimal pendiente = det.getCantidad().subtract(atendida);
            if (pendiente.signum() <= 0) {
                det.setEstado(EstadoSolicitudMovimientoDetalle.ATENDIDO);
                continue;
            }

            // 1) Lote origen con lock
            Long loteId = det.getLote() != null ? det.getLote().getId() : null;
            if (loteId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SIN_LOTE");
            }

            LoteProducto loteOrigen = loteProductoRepository.findByIdForUpdate(loteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO"));

            // 2) Validar stock "movible" = disponible + reserva propia de esta partida
            BigDecimal stockLote = Optional.ofNullable(loteOrigen.getStockLote()).orElse(ZERO);
            BigDecimal stockReservado = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(ZERO);
            BigDecimal movible = stockLote.subtract(stockReservado.subtract(pendiente));

            if (movible.compareTo(pendiente) < 0) {
                log.warn("Stock insuficiente en lote: loteId={} movible={} solicitado={} productoId={}",
                        loteOrigen.getId(), movible, pendiente, producto.getId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_STOCK_INSUFICIENTE");
            }

            // 3) Asegurar/crear lote destino (mismo código), en el almacén destino normalizado
            String codigoLote = null;
            // 1) Preferimos el del loteOrigen (es el que realmente vamos a mover)
            if (loteOrigen != null && loteOrigen.getCodigoLote() != null) {
                codigoLote = loteOrigen.getCodigoLote();
            }
            // 2) Si no, usamos el del lote referenciado por el detalle (si viene cargado)
            else if (det.getLote() != null && det.getLote().getCodigoLote() != null) {
                codigoLote = det.getLote().getCodigoLote();
            }
            // 3) Como último recurso, tomamos el que traiga la solicitud principal
            else if (solicitud != null && solicitud.getCodigoLote() != null) {
                codigoLote = solicitud.getCodigoLote();
            }
            LoteProducto loteDestino = ensureDestinoLote(
                    producto,
                    codigoLote,
                    loteOrigen,
                    almacenDestino
            );

            Almacen destinoAlmacen = Objects.requireNonNull(
                    loteDestino.getAlmacen(),
                    "loteDestino sin almacén asociado"
            );
            //    Usa la misma llamada que ya haces en 'procesarMovimientoConLoteExistente' (mismos args extra).
            ejecutarTransferenciaDesdeLote(
                    loteOrigen,          // Lote de origen
                    destinoAlmacen,      // Almacén destino (del lote destino)
                    producto,            // Producto
                    pendiente,           // Cantidad a mover (lo 'pendiente' del detalle)
                    solicitud            // La solicitud (puede ser null si aplica)
            );

            // 5) Ajustar stock y reservas del origen
            loteOrigen.setStockLote(stockLote.subtract(pendiente));
            loteOrigen.setStockReservado(stockReservado.subtract(pendiente));

            // 6) Marcar la partida como atendida totalmente
            det.setCantidadAtendida(det.getCantidad());
            det.setEstado(EstadoSolicitudMovimientoDetalle.ATENDIDO);

            // Para que se generen los "movimientos hermanos" al final
            result.add(new MovimientoLoteDetalle(loteDestino, pendiente));
        }

        // 7) Cerrar la solicitud (ATENDIDA o PARCIAL)
        boolean todasAtendidas = solicitud.getDetalles().stream()
                .allMatch(d -> d.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO);

        solicitud.setEstado(todasAtendidas
                ? EstadoSolicitudMovimiento.ATENDIDA
                : EstadoSolicitudMovimiento.PARCIAL);

        solicitudMovimientoRepository.saveAndFlush(solicitud);
        return result;
    }

    // =====================================
// CONSUMO AUTOMÁTICO DE INSUMOS POR OP
// =====================================
    @Transactional
    public void consumirInsumosPorOrden(Long ordenProduccionId, Long usuarioId) {

        // 0) IDs de catálogos (existen en tu resolver)
        final Long preBodegaId = Objects.requireNonNull(
                catalogResolver.getAlmacenPreBodegaProduccionId(),
                "CONFIG_FALTANTE: inventory.almacenPreBodegaProduccionId");

        final Long motivoSalidaProdId = Objects.requireNonNull(
                catalogResolver.getMotivoSalidaProduccionId(),
                "CONFIG_FALTANTE: inventory.motivo.salidaProduccionId");

        final Long tipoDetSalidaProdId = Objects.requireNonNull(
                catalogResolver.getTipoDetalleSalidaId(),
                "CONFIG_FALTANTE: inventory.tipoDetalle.salidaProduccionId");

        // 1) Traer todas las solicitudes de esa OP con sus detalles y lotes
        List<SolicitudMovimiento> solicitudes = entityManager.createQuery(
                        "select distinct s " +
                                "from SolicitudMovimiento s " +
                                "left join fetch s.detalles d " +
                                "left join fetch d.lote l " +
                                "where s.ordenProduccion.id = :opId", SolicitudMovimiento.class)
                .setParameter("opId", ordenProduccionId)
                .getResultList();

        if (solicitudes.isEmpty()) {
            log.info("CONSUMO_OP: no hay solicitudes para op={}", ordenProduccionId);
            return;
        }

        // 2) Por cada detalle, generar una SALIDA desde el lote en Pre-Bodega
        for (SolicitudMovimiento sol : solicitudes) {
            final Producto producto = sol.getProducto();
            if (producto == null || producto.getId() == null) {
                log.warn("CONSUMO_OP: solicitud sin producto. solId={}", sol.getId());
                continue;
            }

            for (SolicitudMovimientoDetalle det : sol.getDetalles()) {
                // idempotencia: si ya lo diste por atendido/consumido, salta
                if (det.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO) {
                    continue;
                }

                BigDecimal qty = det.getCantidadAtendida() != null ? det.getCantidadAtendida() : det.getCantidad();
                if (qty == null || qty.signum() <= 0) {
                    continue;
                }

                // código de lote de origen (del detalle o de la solicitud)
                String codigoLote =
                        (det.getLote() != null && det.getLote().getCodigoLote() != null)
                                ? det.getLote().getCodigoLote()
                                : sol.getCodigoLote();

                if (codigoLote == null) {
                    log.warn("CONSUMO_OP: detalle sin codigoLote, solId={}, detId={}", sol.getId(), det.getId());
                    continue;
                }

                // 2.1) Ubicar el lote en Pre-Bodega (mismo código)
                Optional<LoteProducto> lotePreBodegaOpt = loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(
                                codigoLote,
                                producto.getId(),               // Integer requerido por la query
                                preBodegaId.intValue()          // Integer requerido por la query
                        );

                if (lotePreBodegaOpt.isEmpty()) {
                    log.warn("CONSUMO_OP: no existe lote en Pre-Bodega para consumir. op={}, prod={}, lote={}",
                            ordenProduccionId, producto.getId(), codigoLote);
                    continue;
                }

                LoteProducto lotePreBodega = lotePreBodegaOpt.get();

                // 3) Registrar la SALIDA_PRODUCCION (record: usa el orden exacto de tu DTO)
                MovimientoInventarioDTO dtoSalida = new MovimientoInventarioDTO(
                        /* id */                         null,
                        /* cantidad */                   qty,
                        /* tipoMovimiento */             TipoMovimiento.SALIDA,
                        /* clasificacion */              ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                        /* docReferencia */              null,
                        /* destinoTexto */               null,
                        /* productoId */                 producto.getId(),           // Integer
                        /* loteProductoId */             lotePreBodega.getId(),      // Long
                        /* almacenOrigenId */            preBodegaId.intValue(),     // Integer (origen = Pre-Bodega)
                        /* almacenDestinoId */           null,
                        /* proveedorId */                null,
                        /* ordenCompraId */              null,
                        /* motivoMovimientoId */         motivoSalidaProdId,         // Long
                        /* tipoMovimientoDetalleId */    tipoDetSalidaProdId,        // Long
                        /* solicitudMovimientoId */      null,
                        /* usuarioId */                  usuarioId,                  // Long
                        /* ordenProduccionId */          ordenProduccionId,          // Long
                        /* ordenCompraDetalleId */       null,
                        /* codigoLote */                 null,
                        /* fechaVencimiento */           null,
                        /* estadoLote */                 null,
                        /* autoSplit */                  null,
                        /* atenciones */                 null
                );

                // estamos dentro del mismo service
                this.registrarMovimiento(dtoSalida);

                // 4) Marcar el detalle como atendido/consumido (idempotente)
                det.setEstado(EstadoSolicitudMovimientoDetalle.ATENDIDO);
                // No necesitas save explícito: es entidad gestionada en @Transactional
            }
        }
    }

}

