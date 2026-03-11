package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.dto.AtencionDTO;
import com.willyes.clemenintegra.inventario.dto.LoteConsumoDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO;
import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.CondicionProductoDevuelto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoReservaLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.model.enums.ModoControlInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoOrdenCompra;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
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
import org.slf4j.MDC;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereFisico;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereMicro;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereQuimico;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoInventarioServiceImpl.class);
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final long PREBODEGA_PRODUCCION_ID_FALLBACK = 6L;
    /** Nombre normalizado del almacén Pre-Bodega Producción */
    private static final String PRE_BODEGA_PRODUCCION_NORMALIZADO =
            java.text.Normalizer.normalize("Pre-Bodega Producción", java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase();
    private static final Set<RolUsuario> ROLES_OPERATIVOS = EnumSet.of(
            RolUsuario.ROL_JEFE_ALMACENES,
            RolUsuario.ROL_ALMACENISTA,
            RolUsuario.ROL_SUPER_ADMIN
    );
    private static final Set<Long> MOTIVOS_MOVIMIENTO_CRITICOS = Set.of(
            1L,
            2L,
            4L,
            5L,
            9L,
            14L,
            15L,
            16L
    );
    private static final int MAX_BITACORA_VALOR_NUEVO = 255;
    private static final int MAX_BITACORA_OBSERVACION = 500;
    private static final int MAX_DOC_REFERENCIA_LENGTH = 45;
    private static final int CANTIDAD_SCALE = 6;
    private static final RoundingMode CANTIDAD_ROUNDING = RoundingMode.HALF_UP;

    static final record MovimientoLoteDetalle(LoteProducto lote, BigDecimal cantidad) { }

    static final record ParLoteCantidad(Long loteId, BigDecimal cantidad) { }

    private static final record FefoSelection(LoteProducto lote,
                                             BigDecimal disponibleAntes,
                                             BigDecimal tomar,
                                             BigDecimal disponibleDespues) {

        LoteConsumoDTO toDto() {
            return LoteConsumoDTO.builder()
                    .loteId(lote != null ? lote.getId() : null)
                    .codigoLote(lote != null ? lote.getCodigoLote() : null)
                    .fechaVencimiento(lote != null ? lote.getFechaVencimiento() : null)
                    .almacenId(lote != null && lote.getAlmacen() != null
                            ? lote.getAlmacen().getId().longValue()
                            : null)
                    .disponibleAntes(disponibleAntes)
                    .tomar(tomar)
                    .disponibleDespues(disponibleDespues)
                    .build();
        }
    }

    private static final EnumSet<EstadoLote> ESTADOS_FEFO_ELEGIBLES =
            EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO);
    private static final EnumSet<EstadoLote> ESTADOS_FEFO_NO_ELEGIBLES =
            EnumSet.of(EstadoLote.VENCIDO, EstadoLote.RETENIDO, EstadoLote.EN_CUARENTENA);
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
    private final BitacoraCambiosInventarioService bitacoraCambiosInventarioService;
    private final UsuarioService usuarioService;
    private final SolicitudMovimientoRepository solicitudMovimientoRepository;
    private final SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final ReservaLoteService reservaLoteService;
    private final ReservaLoteRepository reservaLoteRepository;
    private final RecepcionOCService recepcionOCService;
    private final LoteCalidadValidator loteCalidadValidator;
    private final UbicacionFisicaRepository ubicacionFisicaRepository;
    private final EtapaProduccionRepository etapaProduccionRepository;
    private final CosteoInventarioService costeoInventarioService;
    //private final Long motivoSalidaProdId = catalogResolver.getMotivoSalidaProduccionId();
    //private final Long tipoDetSalidaProdId = catalogResolver.getTipoDetalleSalidaProduccionId();


    @Resource
    private final EntityManager entityManager;
    @Value("${app.inventario.prebodega.id}")
    private Integer preBodegaId;
    @Value("${inventory.tipoDetalle.transferenciaId}")
    private Integer tipoDetalleTransferenciaId;
    private Long preBodegaProduccionIdCache;
    private boolean preBodegaProduccionIdCacheLoaded;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        return registrarMovimiento(dto, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto, String idempotencyKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Intento de registrar movimiento sin autenticación válida");
            throw new AuthenticationCredentialsNotFoundException("No se encontró autenticación válida");
        }

        if (StringUtils.hasText(idempotencyKey)) {
            repository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
                log.warn("Movimiento duplicado detectado para idempotencyKey={}", idempotencyKey);
                throw new CustomBusinessException(ApiErrorCode.MOVIMIENTO_DUPLICADO,
                        "El movimiento ya fue registrado para esta operación");
            });
        }

        if (dto.tipoMovimiento() == TipoMovimiento.ENTRADA
                && Objects.equals(dto.motivoMovimientoId(), catalogResolver.getMotivoIdEntradaProductoTerminado())
                && dto.ordenProduccionId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID");
        }

        boolean esOpDesdeDto = dto.ordenProduccionId() != null;
        TipoMovimiento tipoMovimiento = dto.tipoMovimiento();
        ClasificacionMovimientoInventario clasificacion = dto.clasificacionMovimientoInventario();
        Long tipoMovimientoDetalleId = dto.tipoMovimientoDetalleId();
        Integer almacenDestinoIdNormalizado = dto.almacenDestinoId();

        boolean esRecepcionDevolucionCliente = tipoMovimiento == TipoMovimiento.RECEPCION
                && clasificacion == ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE;
        boolean loteLegacy = Boolean.TRUE.equals(dto.loteLegacy());

        if (esRecepcionDevolucionCliente) {
            validarRecepcionDevolucionCliente(dto);

            if (!loteLegacy && dto.loteProductoId() == null) {
                throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_LOTE_REQUERIDO,
                        "Debe especificar el lote para la devolución de cliente",
                        Map.of("productoId", dto.productoId()));
            }

            Long almacenPtId = catalogResolver.getAlmacenPtId();
            Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
            if (almacenPtId == null || almacenCuarentenaId == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIG_FALTANTE");
            }

            boolean vaABodegaPt = dto.condicionProductoDevuelto() == CondicionProductoDevuelto.OPTIMO;
            Long destinoId = vaABodegaPt ? almacenPtId : almacenCuarentenaId;
            almacenDestinoIdNormalizado = Math.toIntExact(destinoId);
        }

        // >>> NUEVO: identifica si el cierre de OP está enviando una ENTRADA de PT
        final boolean esEntradaPt = (tipoMovimiento == TipoMovimiento.ENTRADA);

        boolean esRegularizacionTrazabilidad = clasificacion == ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD
                || clasificacion == ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT
                || clasificacion == ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PS;
        if (esOpDesdeDto
                && !esEntradaPt
                && !esRegularizacionTrazabilidad
                && clasificacion != ClasificacionMovimientoInventario.SALIDA_PRODUCCION) {
            tipoMovimiento = TipoMovimiento.TRANSFERENCIA;
            clasificacion = ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
            log.info("OP_NORMALIZED movimiento: tipo={}, clasificacion={}, opId={}, destino={}",
                    tipoMovimiento, clasificacion, dto.ordenProduccionId(), almacenDestinoIdNormalizado);
        }
       // IMPORTANTe: si es ENTRADA de PT, NO tocar tipo/clasificación aquí

        List<AtencionDTO> atenciones = dto.atenciones() != null
                ? dto.atenciones().stream().filter(Objects::nonNull).collect(Collectors.toList())
                : List.of();
        if (!atenciones.isEmpty()) {
            log.debug("MOV-SERVICE atenciones recibidas: {}", atenciones.size());
            if (dto.solicitudMovimientoId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
            }
        }
        /*
         * Ganchos disponibles para enlazar un movimiento con su solicitud/detalle:
         *  - dto.solicitudMovimientoId(): vínculo directo a la cabecera.
         *  - AtencionDTO.detalleId: cada atención mantiene el id del
         *    SolicitudMovimientoDetalle que originó la reserva.
         *  - dto.ordenProduccionId() y dto.loteProductoId(): referencias de contexto
         *    que permiten validar compatibilidad cuando se resuelve la solicitud.
         */
        boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());

        List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> detalleRespuesta = List.of();
        boolean detallesProcesadosPorPartidas = false;

        // 1. Cargar entidades principales
        Producto producto = productoRepository.findById(dto.productoId().longValue())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        Long resolvedTipoDetalleId = dto.tipoMovimientoDetalleId();
        TipoMovimientoDetalle tipoMovimientoDetalle;
        if (resolvedTipoDetalleId != null) {
            final Long tipoDetalleIdFinal = resolvedTipoDetalleId;
            tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(tipoDetalleIdFinal)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE,
                            "Tipo detalle no encontrado",
                            Map.of("tipoMovimientoDetalleId", tipoDetalleIdFinal)));
        } else {
            resolvedTipoDetalleId = esOpDesdeDto
                    ? tipoMovimientoDetalleId
                    : resolveTipoMovimientoDetalleId(dto, producto);
            tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(resolvedTipoDetalleId)
                    .orElseThrow(() -> new NoSuchElementException("Tipo de detalle de movimiento no encontrado"));
        }

        MovimientoInventario movimiento = mapper.toEntity(dto);
        if (StringUtils.hasText(idempotencyKey)) {
            movimiento.setIdempotencyKey(idempotencyKey);
        }
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setClasificacion(clasificacion);
        LocalDateTime fechaIngreso = movimiento.getFechaIngreso();
        if (fechaIngreso == null) {
            fechaIngreso = ZonedDateTime.now(ZONA_BOGOTA).toLocalDateTime();
            movimiento.setFechaIngreso(fechaIngreso);
        }
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);

        if (requiereSolicitudMovimientoId(tipoMovimientoDetalle) && dto.solicitudMovimientoId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
        }

        Integer almacenOrigenIdNormalizadoInt = dto.almacenOrigenId();
        boolean salidaPt = clasificacion != ClasificacionMovimientoInventario.SALIDA_PRODUCCION
                && !esRegularizacionTrazabilidad
                && isSalidaPt(tipoMovimiento, resolvedTipoDetalleId);

        Long almacenPtId = null;
        if (salidaPt) {
            almacenPtId = ensureAlmacenPtId();
            if (dto.almacenOrigenId() != null && !Objects.equals(dto.almacenOrigenId().longValue(), almacenPtId)) {
                log.warn("ALMACEN_ORIGEN_NO_VALIDO_PT dtoOrigenId={} ptId={}", dto.almacenOrigenId(), almacenPtId);
            }
            almacenOrigenIdNormalizadoInt = almacenPtId != null ? Math.toIntExact(almacenPtId) : null;
        }

        Long almacenOrigenIdNormalizado = almacenOrigenIdNormalizadoInt != null
                ? almacenOrigenIdNormalizadoInt.longValue()
                : null;
        Almacen almacenOrigen = almacenOrigenIdNormalizadoInt != null
                ? entityManager.getReference(Almacen.class, almacenOrigenIdNormalizadoInt)
                : null;

        Almacen almacenDestino = almacenDestinoIdNormalizado != null
                ? entityManager.getReference(Almacen.class, almacenDestinoIdNormalizado.longValue()) : null;

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        OrdenProduccion ordenProduccion = dto.ordenProduccionId() != null
                ? entityManager.getReference(OrdenProduccion.class, dto.ordenProduccionId())
                : null;
        EtapaProduccion etapaProduccion = null;
        Integer almacenParaUbicacion = almacenDestino != null
                ? almacenDestino.getId()
                : (almacenOrigen != null ? almacenOrigen.getId() : null);
        UbicacionFisica ubicacionFisicaDestino = null;
        if (dto.ubicacionDestinoId() != null) {
            Long ubicacionId = dto.ubicacionDestinoId();
            ubicacionFisicaDestino = ubicacionFisicaRepository.findByIdAndActivoTrue(ubicacionId)
                    .orElseThrow(() -> new CustomBusinessException(
                            ApiErrorCode.UBICACION_NO_ENCONTRADA,
                            "La ubicación destino no existe o está inactiva",
                            Map.of("ubicacionId", ubicacionId)
                    ));
            Integer almacenUbicacion = ubicacionFisicaDestino.getAlmacen() != null
                    ? ubicacionFisicaDestino.getAlmacen().getId()
                    : null;
            Long almacenDestinoDetalle = almacenDestinoIdNormalizado != null
                    ? almacenDestinoIdNormalizado.longValue()
                    : almacenOrigenIdNormalizado;
            if (almacenParaUbicacion == null || !Objects.equals(almacenParaUbicacion, almacenUbicacion)) {
                throw new CustomBusinessException(
                        ApiErrorCode.UBICACION_NO_PERTENECE_ALMACEN,
                        "La ubicación no pertenece al almacén indicado",
                        Map.of(
                                "ubicacionId", ubicacionId,
                                "almacenDestinoId", almacenDestinoDetalle,
                                "almacenUbicacionId", almacenUbicacion
                        )
                );
            }
        }

        log.debug("MOV-REQ (pre-solicitud) tipo={}, clasificacion={}, prod={}, qty={}, opIdDTO={}",
                tipoMovimiento, clasificacion, dto.productoId(), dto.cantidad(), dto.ordenProduccionId());

        Long solicitudIdReferencia = dto.solicitudMovimientoId();
        SolicitudMovimiento solicitud = null;
        if (solicitudIdReferencia != null) {
            final Long solicitudIdCarga = solicitudIdReferencia;
            solicitud = solicitudMovimientoRepository.findByIdWithLock(solicitudIdCarga)
                    .orElseGet(() -> solicitudMovimientoRepository.findWithDetalles(solicitudIdCarga)
                            .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada")));

            Long solicitudProductoId = solicitud.getProducto() != null ? Long.valueOf(solicitud.getProducto().getId()) : null;
            Long productoDtoId = dto.productoId() != null ? dto.productoId().longValue() : null;
            if (solicitudProductoId != null
                    && productoDtoId != null
                    && !Objects.equals(solicitudProductoId, productoDtoId)) {
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

            Long solicitudAlmacenOrigenId = solicitud.getAlmacenOrigen() != null
                    ? Long.valueOf(solicitud.getAlmacenOrigen().getId()) : null;
            Long dtoAlmacenOrigenId = almacenOrigenIdNormalizado;
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
                    || solicitud.getEstado() == EstadoSolicitudMovimiento.CANCELADA
                    || solicitud.getEstado() == EstadoSolicitudMovimiento.CERRADA) {
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
        if (solicitud != null && !atenciones.isEmpty()) {
            validarAtencionesConSolicitud(dto, solicitud, atenciones);
        }
        // === /OP OVERRIDES ===
        Long ordenProduccionIdContexto = dto.ordenProduccionId();
        if (ordenProduccionIdContexto == null && ordenProduccion != null) {
            ordenProduccionIdContexto = ordenProduccion.getId();
        }
        if (ordenProduccionIdContexto == null
                && solicitud != null
                && solicitud.getOrdenProduccion() != null
                && solicitud.getOrdenProduccion().getId() != null) {
            ordenProduccionIdContexto = solicitud.getOrdenProduccion().getId();
        }

        Long preBodegaProduccionId = resolverAlmacenPreBodegaId();
        boolean esTrasladoAPreBodega = isTrasladoAPrebodega(
                tipoMovimiento,
                clasificacion,
                tipoMovimientoDetalle,
                almacenDestinoIdNormalizado,
                almacenDestino,
                preBodegaProduccionId
        );
        if (esTrasladoAPreBodega) {
            if (preBodegaProduccionId != null
                    && (almacenDestino == null
                    || !Objects.equals(almacenDestino.getId().longValue(), preBodegaProduccionId))) {
                almacenDestino = entityManager.getReference(Almacen.class, Math.toIntExact(preBodegaProduccionId));
                almacenDestinoIdNormalizado = almacenDestino.getId();
            }
            if (tipoMovimiento != TipoMovimiento.TRANSFERENCIA) {
                tipoMovimiento = TipoMovimiento.TRANSFERENCIA;
                movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
            }
            if (clasificacion != ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION) {
                clasificacion = ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
                movimiento.setClasificacion(clasificacion);
            }
            if (tipoDetalleTransferenciaId != null
                    && (tipoMovimientoDetalle == null
                    || !Objects.equals(tipoMovimientoDetalle.getId(), tipoDetalleTransferenciaId.longValue()))) {
                tipoMovimientoDetalle = tipoMovimientoDetalleRepository
                        .findById(tipoDetalleTransferenciaId.longValue())
                        .orElse(tipoMovimientoDetalle);
                movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);
            }
            resolvedTipoDetalleId = tipoMovimientoDetalle != null
                    ? tipoMovimientoDetalle.getId()
                    : resolvedTipoDetalleId;
            log.info("TRASLADO_PREBODEGA_DETECTADO: opId={} destinoId={} tipoNormalizado={} clasificacion={}",
                    ordenProduccionIdContexto, almacenDestinoIdNormalizado, tipoMovimiento, clasificacion);
        }

        boolean esConsumoEtapa = clasificacion == ClasificacionMovimientoInventario.SALIDA_PRODUCCION;
        boolean bloquearEtapaPorTraslado = esTrasladoAPreBodega;
        boolean requiereEtapaActiva = !esTrasladoAPreBodega && requiereEtapaActiva(
                clasificacion,
                resolvedTipoDetalleId,
                dto.ordenProduccionEtapaId(),
                ordenProduccionIdContexto);
        if (requiereEtapaActiva) {
            EtapaProduccion etapaActiva = resolverEtapaActiva(ordenProduccionIdContexto, dto.ordenProduccionEtapaId());
            etapaProduccion = etapaActiva;
        }

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

        validarUbicacionDestinoTransferenciaPt(dto, tipoMovimiento, clasificacion, almacenOrigen, almacenDestino);

        Long motivoMovimientoId = dto.motivoMovimientoId();
        if (motivoMovimientoId == null && tipoMovimiento == TipoMovimiento.TRANSFERENCIA) {
            ClasificacionMovimientoInventario clasificacionMotivo = clasificacion != null
                    ? clasificacion
                    : ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL;
            motivoMovimientoId = motivoMovimientoRepository.findByMotivo(clasificacionMotivo)
                    .or(() -> motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL))
                    .map(MotivoMovimiento::getId)
                    .orElseThrow(() -> new CustomBusinessException(
                            ApiErrorCode.CATALOGO_FALTANTE,
                            "No se encontró el motivo de movimiento para transferencias",
                            Map.of("clasificacion", clasificacionMotivo.name())
                    ));
        }
        if (motivoMovimientoId == null && esRecepcionDevolucionCliente) {
            motivoMovimientoId = resolverMotivoDevolucionCliente(almacenDestino);
        }
        if (motivoMovimientoId == null && esRecepcionDevolucionCliente) {
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS,
                    "Debe indicar un motivo para registrar la devolución",
                    Map.of("productoId", dto.productoId()));
        }

        MotivoMovimiento motivoMovimiento = null;
        if (motivoMovimientoId != null) {
            final Long motivoMovimientoIdFinal = motivoMovimientoId;
            motivoMovimiento = motivoMovimientoRepository.findById(motivoMovimientoId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE,
                            "Motivo no encontrado",
                            Map.of("motivoMovimientoId", motivoMovimientoIdFinal)));
        }
        motivoMovimiento = resolverMotivoMovimientoPorClasificacion(clasificacion, motivoMovimiento);

        OrdenCompra orden = null;
        RecepcionOC recepcionCabecera = null;
        if (tipoMovimiento == TipoMovimiento.RECEPCION
                && motivoMovimiento != null
                && motivoMovimiento.getMotivo() == ClasificacionMovimientoInventario.RECEPCION_COMPRA) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Se requiere una orden de compra para la recepción de compra");
            }
            orden = ordenCompraRepository.findById(dto.ordenCompraId().longValue())
                    .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada"));
            if (orden.getTipo() == TipoOrdenCompra.SERVICIOS) {
                throw new CustomBusinessException(ApiErrorCode.OC_SERVICIO_NO_RECEPCIONABLE,
                        "Las órdenes de tipo SERVICIOS no pueden recepcionarse por inventario");
            }
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
            Integer almacenDestinoIdRequerido = Objects.requireNonNull(almacenDestinoIdNormalizado,
                    "Se requiere un almacén destino para la recepción");
            LocalDate fechaNegocio = fechaIngreso.atZone(ZONA_BOGOTA).toLocalDate();
            String observacionesCabecera = dto.destinoTexto() != null && !dto.destinoTexto().isBlank()
                    ? dto.destinoTexto()
                    : dto.docReferencia();
            recepcionCabecera = recepcionOCService.findOrCreateCabecera(
                    dto.ordenCompraId(),
                    almacenDestinoIdRequerido,
                    dto.proveedorId(),
                    usuario.getId(),
                    fechaNegocio,
                    observacionesCabecera
            );
            movimiento.setRecepcionOc(recepcionCabecera);
            movimiento.setCodigoRecepcion(recepcionCabecera.getCodigo());
        }

        BigDecimal cantidadSolicitada = normalizarCantidad(dto.cantidad());
        List<MovimientoLoteDetalle> lotesProcesados;
        AtomicBoolean solicitudOpProcesadaEnLote = new AtomicBoolean(false);

        boolean solicitudConPartidas = solicitud != null
                && solicitud.getDetalles() != null
                && !solicitud.getDetalles().isEmpty()
                && !esContextoOrdenProduccion(dto, solicitud);

        if (esRecepcionDevolucionCliente) {
            lotesProcesados = procesarRecepcionDevolucionCliente(dto, producto, cantidadSolicitada, almacenDestino, loteLegacy);
        } else if (tipoMovimiento == TipoMovimiento.RECEPCION) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Las recepciones sin Orden de Compra deben usar un lote existente");
            }
            OrdenCompraDetalle detalleCostoRecepcion = resolverDetalleOrdenCompra(dto, orden, producto);
            BigDecimal gastosAdicionalesRecepcion = recepcionCabecera != null
                    ? safeScale6(recepcionCabecera.getGastosAdicionalesTotal())
                    : BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal subtotalTotalRecepcionConIva = calcularSubtotalLineaConIva(detalleCostoRecepcion, cantidadSolicitada);
            LoteProducto loteRecepcion = crearLoteRecepcion(dto, producto, almacenDestino, usuario,
                    cantidadSolicitada, motivoMovimiento, detalleCostoRecepcion, gastosAdicionalesRecepcion, subtotalTotalRecepcionConIva);
            lotesProcesados = List.of(new MovimientoLoteDetalle(loteRecepcion, cantidadSolicitada));

        } else if (salidaPt) {
            lotesProcesados = procesarSalidaPt(dto, producto, cantidadSolicitada,
                    almacenPtId, atenciones, autoSplitSolicitado);

        } else if (tipoMovimiento == TipoMovimiento.SALIDA
                && clasificacion == ClasificacionMovimientoInventario.SALIDA_PRODUCCION) {

            // Consumir directamente del lote (ya en Pre-Bodega).
            lotesProcesados = procesarMovimientoConLoteExistente(
                    dto,
                    tipoMovimiento,
                    clasificacion,
                    almacenOrigen,
                    almacenDestino,
                    producto,
                    cantidadSolicitada,
                    devolucionInterna,
                    /* solicitud */ solicitud,
                    solicitudOpProcesadaEnLote
            );

        } else if (solicitudConPartidas) {
            // ⬅️ NUEVO: aprobar por partidas (por cada lote del detalle)
            lotesProcesados = procesarMovimientoPorPartidas(
                    solicitud,
                    producto,
                    almacenDestino,
                    tipoMovimiento,
                    devolucionInterna
            );
            detallesProcesadosPorPartidas = true;

        } else {
            // Comportamiento anterior (un solo lote desde DTO)
            lotesProcesados = procesarMovimientoConLoteExistente(
                    dto, tipoMovimiento, clasificacion, almacenOrigen, almacenDestino,
                    producto, cantidadSolicitada, devolucionInterna, solicitud, solicitudOpProcesadaEnLote
            );
        }

        if (lotesProcesados == null || lotesProcesados.isEmpty()) {
            throw new IllegalStateException("No se generaron lotes para el movimiento");
        }

        if (ubicacionFisicaDestino != null && almacenParaUbicacion != null) {
            for (MovimientoLoteDetalle detalle : lotesProcesados) {
                LoteProducto loteDestino = detalle.lote();
                if (loteDestino != null
                        && loteDestino.getAlmacen() != null
                        && Objects.equals(loteDestino.getAlmacen().getId(), almacenParaUbicacion)) {
                    loteDestino.setUbicacionFisica(ubicacionFisicaDestino);
                    loteProductoRepository.save(loteDestino);
                }
            }
        }

        boolean solicitudOp = solicitud != null && esContextoOrdenProduccion(dto, solicitud);
        boolean solicitudOpProcesada = solicitudOpProcesadaEnLote.get();
        boolean respuestaIdempotente = solicitudOp
                && solicitudOpProcesada
                && lotesProcesados.size() == 1
                && (lotesProcesados.get(0).cantidad() == null
                || lotesProcesados.get(0).cantidad().compareTo(BigDecimal.ZERO) == 0);

        if (respuestaIdempotente) {
            List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> detallesRespuesta =
                    construirRespuestaSolicitud(solicitud);
            return MovimientoInventarioResponseDTO.builder()
                    .solicitudId(solicitud.getId())
                    .estadoSolicitud(solicitud.getEstado())
                    .ordenProduccionId(solicitud.getOrdenProduccion() != null
                            ? solicitud.getOrdenProduccion().getId()
                            : null)
                    .codigoOrdenProduccion(solicitud.getOrdenProduccion() != null
                            ? solicitud.getOrdenProduccion().getCodigoOrden()
                            : null)
                    .ordenProduccionEtapaId(dto.ordenProduccionEtapaId())
                    .nombreEtapaProduccion(resolverNombreEtapaProduccion(dto.ordenProduccionEtapaId()))
                    .detallesSolicitud(detallesRespuesta == null ? List.of() : List.copyOf(detallesRespuesta))
                    .build();
        }

        if (solicitud != null
                && solicitudOp
                && !detallesProcesadosPorPartidas
                && !solicitudOpProcesada) {
            detalleRespuesta = aplicarContraSolicitudMovimiento(dto, solicitud);
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
        movimiento.setCantidad(normalizarCantidad(principal.cantidad()));
        aplicarCostoMovimiento(movimiento, principal.lote(), principal.cantidad());
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setOrdenProduccion(ordenProduccion);
        if (!bloquearEtapaPorTraslado) {
            movimiento.setOrdenProduccionEtapa(etapaProduccion);
        }
        if (esConsumoEtapa && movimiento.getOrdenProduccionEtapa() == null && ordenProduccionIdContexto != null) {
            Long etapaId = resolverEtapaConsumo(ordenProduccionIdContexto, null);
            movimiento.setOrdenProduccionEtapa(entityManager.getReference(EtapaProduccion.class, etapaId));
        }
        movimiento.setProveedor(dto.proveedorId() != null
                ? entityManager.getReference(Proveedor.class, dto.proveedorId()) : null);
        movimiento.setOrdenCompra(dto.ordenCompraId() != null
                ? entityManager.getReference(OrdenCompra.class, dto.ordenCompraId()) : null);
        movimiento.setOrdenCompraDetalle(ordenCompraDetalle);
        movimiento.setMotivoMovimiento(motivoMovimiento);
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle);
        movimiento.setRegistradoPor(usuario);
        if (solicitud != null) {
            movimiento.setSolicitudMovimiento(solicitud);
        }
        if (esConsumoEtapa
                && movimiento.getOrdenProduccionEtapa() == null
                && movimiento.getSolicitudMovimiento() != null
                && movimiento.getSolicitudMovimiento().getOrdenProduccion() != null
                && movimiento.getSolicitudMovimiento().getOrdenProduccion().getId() != null) {
            Long etapaId = resolverEtapaConsumo(movimiento.getSolicitudMovimiento().getOrdenProduccion().getId(), null);
            movimiento.setOrdenProduccionEtapa(entityManager.getReference(EtapaProduccion.class, etapaId));
        }

        if (clasificacion == ClasificacionMovimientoInventario.SALIDA_CLIENTE) {
            log.debug("SALIDA_CLIENTE motivoMovimientoId={}",
                    movimiento.getMotivoMovimiento() != null ? movimiento.getMotivoMovimiento().getId() : null);
        }

        if (ordenProduccion != null && StringUtils.hasText(ordenProduccion.getCodigoOrden())) {
            movimiento.setDocReferencia(ordenProduccion.getCodigoOrden().trim());
        }

        String docReferenciaFinal = movimiento.getDocReferencia();
        if (docReferenciaFinal != null) {
            docReferenciaFinal = docReferenciaFinal.trim();
            if (docReferenciaFinal.isBlank()) {
                movimiento.setDocReferencia(null);
            } else {
                if (docReferenciaFinal.length() > MAX_DOC_REFERENCIA_LENGTH) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DOC_REFERENCIA_LARGA");
                }
                movimiento.setDocReferencia(docReferenciaFinal);
            }
        }

        MovimientoInventario guardado = repository.save(movimiento);
        registrarBitacoraMovimientoCritico(guardado, usuario, idempotencyKey);

        if (solicitud != null) {
            if (detalleRespuesta == null || detalleRespuesta.isEmpty()) {
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
        }

        if (lotesProcesados.size() > 1) {
            List<MovimientoInventario> adicionales = new ArrayList<>();
            for (int i = 1; i < lotesProcesados.size(); i++) {
                MovimientoLoteDetalle detalle = lotesProcesados.get(i);
                MovimientoInventario movimientoAdicional = duplicarMovimientoBase(movimiento, detalle.lote(), detalle.cantidad());
                aplicarCostoMovimiento(movimientoAdicional, detalle.lote(), detalle.cantidad());
                adicionales.add(movimientoAdicional);
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

    private void registrarBitacoraMovimientoCritico(MovimientoInventario movimiento,
                                                    Usuario usuario,
                                                    String idempotencyKey) {
        if (movimiento == null) {
            return;
        }
        MotivoMovimiento motivoMovimiento = movimiento.getMotivoMovimiento();
        Long motivoId = motivoMovimiento != null ? motivoMovimiento.getId() : null;
        if (motivoId == null || !MOTIVOS_MOVIMIENTO_CRITICOS.contains(motivoId)) {
            return;
        }
        if (usuario == null || usuario.getId() == null) {
            log.warn("BITACORA_MOVIMIENTO_OMITIDA: usuario no disponible para movimientoId={}",
                    movimiento.getId());
            return;
        }
        String valorNuevo = buildResumenMovimiento(movimiento, motivoId);
        String observacion = buildObservacionMovimiento(movimiento.getDocReferencia(), idempotencyKey);
        try {
            bitacoraCambiosInventarioService.crear(BitacoraCambiosInventarioDTO.builder()
                    .tablaAfectada("movimientos_inventario")
                    .registroId(movimiento.getId() != null ? movimiento.getId() : 0L)
                    .campoModificado("movimiento")
                    .valorAnt("N/A")
                    .valorNuevo(valorNuevo)
                    .accion("MOVIMIENTO_CRITICO_REGISTRADO")
                    .observacion(observacion)
                    .fechaCambio(LocalDateTime.now())
                    .usuarioId(usuario.getId())
                    .usuarioNombre(resolveNombreUsuario(usuario))
                    .build());
        } catch (Exception ex) {
            log.warn("BITACORA_MOVIMIENTO_ERROR: movimientoId={} motivoId={} msg={}",
                    movimiento.getId(), motivoId, ex.getMessage(), ex);
        }
    }

    private String buildResumenMovimiento(MovimientoInventario movimiento, Long motivoId) {
        String cantidad = movimiento.getCantidad() != null ? movimiento.getCantidad().toPlainString() : "N/A";
        String loteId = movimiento.getLote() != null && movimiento.getLote().getId() != null
                ? movimiento.getLote().getId().toString()
                : "N/A";
        String almacenOrigenId = movimiento.getAlmacenOrigen() != null && movimiento.getAlmacenOrigen().getId() != null
                ? movimiento.getAlmacenOrigen().getId().toString()
                : "N/A";
        String almacenDestinoId = movimiento.getAlmacenDestino() != null && movimiento.getAlmacenDestino().getId() != null
                ? movimiento.getAlmacenDestino().getId().toString()
                : "N/A";
        String tipoDetId = movimiento.getTipoMovimientoDetalle() != null
                && movimiento.getTipoMovimientoDetalle().getId() != null
                ? movimiento.getTipoMovimientoDetalle().getId().toString()
                : "N/A";
        String motivoValue = motivoId != null ? motivoId.toString() : "N/A";
        String resumen = String.format(
                "cant=%s, loteId=%s, orig=%s, dest=%s, tipoDetId=%s, motivoId=%s",
                cantidad,
                loteId,
                almacenOrigenId,
                almacenDestinoId,
                tipoDetId,
                motivoValue
        );
        return truncate(resumen, MAX_BITACORA_VALOR_NUEVO);
    }

    private String buildObservacionMovimiento(String docReferencia, String idempotencyKey) {
        String docRef = safeTexto(docReferencia);
        String idempotencia = safeTexto(idempotencyKey);
        String observacion = docRef + " | " + idempotencia;
        return truncate(observacion, MAX_BITACORA_OBSERVACION);
    }

    private String safeTexto(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return "N/A";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "N/A";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String resolveNombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return "N/A";
        }
        if (StringUtils.hasText(usuario.getNombreCompleto())) {
            return usuario.getNombreCompleto().trim();
        }
        if (StringUtils.hasText(usuario.getNombreUsuario())) {
            return usuario.getNombreUsuario().trim();
        }
        return "N/A";
    }

    private boolean esContextoOrdenProduccion(MovimientoInventarioDTO dto, SolicitudMovimiento solicitud) {
        if (dto != null && dto.ordenProduccionId() != null) {
            return true;
        }
        return solicitud != null && solicitud.getOrdenProduccion() != null;
    }

    private List<MovimientoInventarioResponseDTO.SolicitudDetalleAtencionDTO> aplicarContraSolicitudMovimiento(
            MovimientoInventarioDTO dto,
            SolicitudMovimiento solicitud
    ) {
        entityManager.lock(solicitud, LockModeType.PESSIMISTIC_WRITE);

        EstadoSolicitudMovimiento estadoActual = solicitud.getEstado();
        if (estadoActual != null && !EnumSet.of(
                EstadoSolicitudMovimiento.PENDIENTE,
                EstadoSolicitudMovimiento.AUTORIZADA,
                EstadoSolicitudMovimiento.PARCIAL,
                EstadoSolicitudMovimiento.RESERVADA
        ).contains(estadoActual)) {
            log.warn("SOLICITUD_YA_ATENDIDA: solId={} estado={}", solicitud.getId(), estadoActual);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SOLICITUD_YA_ATENDIDA");
        }

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

            SolicitudMovimientoDetalle detalle = obtenerDetalleParaAtencion(solicitud, atencion);
            if (detalle == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_NO_COMPATIBLE");
            }
            validarDetalleCompleto(solicitud, detalle);

            Long loteId = detalle.getLote().getId();

            EstadoSolicitudMovimientoDetalle estadoDetalle = detalle.getEstado();
            if (estadoDetalle != null && !EnumSet.of(
                    EstadoSolicitudMovimientoDetalle.PENDIENTE,
                    EstadoSolicitudMovimientoDetalle.PARCIAL
            ).contains(estadoDetalle)) {
                log.warn("DETALLE_YA_ATENDIDO: solicitudId={} detalleId={} estado={}",
                        solicitud.getId(), detalle.getId(), estadoDetalle);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "DETALLE_YA_ATENDIDO");
            }

            BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal atendidaPrev = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal pendienteDetalle = solicitada.subtract(atendidaPrev);
            if (pendienteDetalle.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("DETALLE_SIN_PENDIENTE solicitudId={} detalleId={}", solicitud.getId(), detalle.getId());
                throw new ResponseStatusException(HttpStatus.CONFLICT, "DETALLE_YA_ATENDIDO");
            }
            if (cantidad.compareTo(pendienteDetalle) > 0) {
                log.warn("ATENCION_EXCEDE_PENDIENTE solicitudId={} detalleId={} aprobado={} pendiente={}",
                        solicitud.getId(), detalle.getId(), cantidad, pendienteDetalle);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "ATENCION_CANTIDAD_EXCEDE_PENDIENTE");
            }

            LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

            reservaLoteService.consumirReserva(solicitud, detalle, lote, cantidad);

            BigDecimal stockAntes = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
            BigDecimal reservadoAntes = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
            log.debug("VAL-ACTUALIZA (OP) antes actualizarStockLote loteId={} stockAntes={} reservadoAntes={} req={}",
                    lote.getId(), stockAntes, reservadoAntes, cantidad);

            actualizarStockLote(lote, cantidad, lote.getProducto());
            loteProductoRepository.save(lote);

            actualizarDetalleSolicitud(detalle, cantidad);
            solicitudMovimientoDetalleRepository.save(detalle);
        }

        actualizarEstadoSolicitud(solicitud);
        solicitudMovimientoRepository.saveAndFlush(solicitud);
        return construirRespuestaSolicitud(solicitud);
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

            SolicitudMovimientoDetalle detalle = obtenerDetalleParaAtencion(solicitud, atencion);
            Long loteId = detalle != null && detalle.getLote() != null ? detalle.getLote().getId() : atencion.getLoteId();
            if (detalle != null) {
                validarDetalleCompleto(solicitud, detalle);
                BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO)
                        .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
                BigDecimal atendidaPrev = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO)
                        .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
                BigDecimal pendienteDetalle = solicitada.subtract(atendidaPrev);
                if (pendienteDetalle.compareTo(BigDecimal.ZERO) < 0) {
                    pendienteDetalle = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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

            if (loteId == null) {
                throw new CustomBusinessException(
                        ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                        "SOLICITUD_DETALLE_INCOMPLETO",
                        Map.of(
                                "solicitudId", solicitud.getId(),
                                "detalleId", detalle != null ? detalle.getId() : null,
                                "missingFields", List.of("lote_id")
                        )
                );
            }

            LoteProducto lote = loteProductoRepository.findByIdForUpdate(loteId)
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));

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
            validarAtencionesConSolicitud(dto, solicitud, atenciones);
            log.debug("SOLICITUD atenciones recibidas: {}", atenciones.size());
            return atenciones;
        }

        if (dto.solicitudMovimientoId() == null || solicitud == null) {
            return List.of();
        }

        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_DETALLE_REQUERIDO");
    }

    private void validarAtencionesConSolicitud(MovimientoInventarioDTO dto,
                                               SolicitudMovimiento solicitud,
                                               List<AtencionDTO> atenciones) {
        Long solicitudId = dto != null ? dto.solicitudMovimientoId() : (solicitud != null ? solicitud.getId() : null);
        if (solicitudId == null) {
            if (!atenciones.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
            }
            return;
        }

        long totalSinDetalle = atenciones.stream()
                .filter(atencion -> atencion != null && atencion.getDetalleId() == null)
                .count();
        if (totalSinDetalle == 0) {
            return;
        }

        boolean solicitudOpConDetalles = solicitud != null
                && solicitud.getOrdenProduccion() != null
                && solicitud.getDetalles() != null
                && !solicitud.getDetalles().isEmpty();

        for (AtencionDTO atencion : atenciones) {
            if (atencion == null || atencion.getDetalleId() != null) {
                continue;
            }
            if (solicitudOpConDetalles && atencion.getCantidad() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "ATENCION_CANTIDAD_REQUERIDA");
            }
            Long loteId = atencion.getLoteId() != null
                    ? atencion.getLoteId()
                    : (dto != null ? dto.loteProductoId() : null);
            Long almacenOrigenId = atencion.getAlmacenOrigenId() != null
                    ? atencion.getAlmacenOrigenId().longValue()
                    : (dto != null && dto.almacenOrigenId() != null ? dto.almacenOrigenId().longValue() : null);
            Long almacenDestinoId = atencion.getAlmacenDestinoId() != null
                    ? atencion.getAlmacenDestinoId().longValue()
                    : (dto != null && dto.almacenDestinoId() != null ? dto.almacenDestinoId().longValue() : null);
            BigDecimal cantidad = atencion.getCantidad() != null
                    ? atencion.getCantidad()
                    : (dto != null ? dto.cantidad() : null);
            Long detalleId = resolveDetalleIdOrThrow(
                    solicitudId,
                    loteId,
                    almacenOrigenId,
                    almacenDestinoId,
                    cantidad);
            atencion.setDetalleId(detalleId);
            log.info("SOLICITUD_DETALLE_RESUELTO: solicitudId={} detalleId={} cantidadSolicitada={}",
                    solicitudId, detalleId, atencion.getCantidad());
        }
    }

    private void validarAtencionCompatibleConDetalle(Long solicitudId,
                                                     AtencionDTO atencion,
                                                     SolicitudMovimientoDetalle detalle) {
        Long loteDetalleId = detalle.getLote() != null ? detalle.getLote().getId() : null;
        if (atencion.getLoteId() != null && loteDetalleId != null
                && !Objects.equals(atencion.getLoteId(), loteDetalleId)) {
            log.warn("SOLICITUD_DETALLE_MISMATCH lote: solicitudId={} detalleId={} loteDetalle={} loteAtencion={}",
                    solicitudId, detalle.getId(), loteDetalleId, atencion.getLoteId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_DETALLE_MISMATCH");
        }

        Long almacenDetalleId = detalle.getAlmacenOrigen() != null
                ? Long.valueOf(detalle.getAlmacenOrigen().getId())
                : null;
        if (atencion.getAlmacenOrigenId() != null && almacenDetalleId != null
                && !Objects.equals(atencion.getAlmacenOrigenId().longValue(), almacenDetalleId)) {
            log.warn("SOLICITUD_DETALLE_MISMATCH almacen: solicitudId={} detalleId={} almacenDetalle={} almacenAtencion={}",
                    solicitudId, detalle.getId(), almacenDetalleId, atencion.getAlmacenOrigenId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_DETALLE_MISMATCH");
        }

        BigDecimal cantidadAtencion = normalizarCantidad(atencion.getCantidad());
        BigDecimal cantidadDetalle = detalle.getCantidad() != null
                ? detalle.getCantidad().setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING)
                : null;
        if (cantidadAtencion != null && cantidadDetalle != null && cantidadAtencion.compareTo(cantidadDetalle) > 0) {
            log.warn("SOLICITUD_DETALLE_MISMATCH cantidad: solicitudId={} detalleId={} detalleCantidad={} atencionCantidad={}",
                    solicitudId, detalle.getId(), cantidadDetalle, cantidadAtencion);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_DETALLE_MISMATCH");
        }
    }



    private Long resolverLoteIdSolicitud(MovimientoInventarioDTO dto, SolicitudMovimiento solicitud) {
        if (solicitud == null) {
            return dto.loteProductoId();
        }

        List<AtencionDTO> atenciones = dto.atenciones() != null
                ? dto.atenciones().stream().filter(Objects::nonNull).collect(Collectors.toList())
                : List.of();
        if (!atenciones.isEmpty()) {
            validarAtencionesConSolicitud(dto, solicitud, atenciones);
            AtencionDTO atencion = atenciones.get(0);
            SolicitudMovimientoDetalle detalle = obtenerDetalleParaAtencion(solicitud, atencion);
            if (detalle == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_NO_COMPATIBLE");
            }
            validarDetalleConLote(solicitud.getId(), detalle);
            return detalle.getLote().getId();
        }

        List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles()).orElse(List.of());
        if (detalles.size() == 1) {
            SolicitudMovimientoDetalle detalle = detalles.get(0);
            validarDetalleConLote(solicitud.getId(), detalle);
            return detalle.getLote().getId();
        }
        if (!detalles.isEmpty()) {
            throw new CustomBusinessException(
                    ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                    "SOLICITUD_DETALLE_INCOMPLETO",
                    Map.of(
                            "solicitudId", solicitud.getId(),
                            "detalleId", null,
                            "missingFields", List.of("detalle_id")
                    )
            );
        }
        return dto.loteProductoId();
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

            validarDetalleCompleto(dto != null ? dto.solicitudMovimientoId() : null, detalle);

            BigDecimal solicitada = Optional.ofNullable(detalle.getCantidad())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal pendiente = solicitada.subtract(atendida);
            if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
                pendiente = BigDecimal.ZERO;
            }
            pendiente = pendiente.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            if (pendiente.compareTo(cero) <= 0) {
                continue;
            }

            BigDecimal cantidadAtencion = pendiente;
            if (restanteTotal != null && cantidadAtencion.compareTo(restanteTotal) > 0) {
                cantidadAtencion = restanteTotal;
            }
            cantidadAtencion = cantidadAtencion.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            if (cantidadAtencion.compareTo(cero) <= 0) {
                continue;
            }

            Long detalleLoteId = detalle.getLote() != null ? detalle.getLote().getId() : null;

            AtencionDTO generado = new AtencionDTO();
            generado.setDetalleId(detalle.getId());
            generado.setLoteId(detalleLoteId);
            generado.setCantidad(cantidadAtencion);
            generado.setAlmacenOrigenId(
                    detalle.getAlmacenOrigen() != null
                            ? (detalle.getAlmacenOrigen().getId() != null ? detalle.getAlmacenOrigen().getId().intValue() : null)
                            : null
            );

            Integer destinoAtencion =
                    (detalle.getAlmacenDestino() != null
                            ? (detalle.getAlmacenDestino().getId() != null ? detalle.getAlmacenDestino().getId().intValue() : null)
                            : null
                    );

            generado.setAlmacenDestinoId(destinoAtencion);
            generadas.add(generado);


            if (restanteTotal != null) {
                restanteTotal = restanteTotal.subtract(cantidadAtencion).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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
        return valor.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private CustomBusinessException loteStockInsuficienteException(LoteProducto lote,
                                                                   Producto producto,
                                                                   BigDecimal solicitado,
                                                                   BigDecimal disponible,
                                                                   Almacen almacenOrigen) {
        BigDecimal solicitadoNorm = Optional.ofNullable(normalizarCantidad(solicitado))
                .orElse(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        BigDecimal disponibleNorm = Optional.ofNullable(normalizarCantidad(disponible))
                .orElse(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        BigDecimal diferencia = solicitadoNorm.subtract(disponibleNorm).max(BigDecimal.ZERO)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);

        Map<String, Object> details = new HashMap<>();
        details.put("code", "LOTE_STOCK_INSUFICIENTE");
        details.put("productoId", producto != null ? producto.getId() : null);
        details.put("loteId", lote != null ? lote.getId() : null);
        details.put("solicitado", solicitadoNorm);
        details.put("disponible", disponibleNorm);
        details.put("diferencia", diferencia);
        details.put("almacenOrigenId", almacenOrigen != null ? almacenOrigen.getId() : null);

        return new CustomBusinessException(
                ApiErrorCode.LOTE_STOCK_INSUFICIENTE,
                "LOTE_STOCK_INSUFICIENTE",
                details
        );
    }

    private void actualizarStockLote(LoteProducto lote, BigDecimal cantidad, Producto producto) {
        BigDecimal stockActual = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal cantidadStock = Optional.ofNullable(cantidad).orElse(BigDecimal.ZERO);
        BigDecimal nuevoStock = stockActual.subtract(cantidadStock);
        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STOCK_LOTE_INSUFICIENTE");
        }

        BigDecimal reservadoPendiente = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        if (lote.getId() != null) {
            reservadoPendiente = Optional.ofNullable(
                            reservaLoteRepository.sumPendienteActivaByLoteId(lote.getId(), EstadoReservaLote.ACTIVA))
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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
        return Math.max(CANTIDAD_SCALE, catalogResolver.decimals(producto != null ? producto.getUnidadMedida() : null));
    }

    private SolicitudMovimientoDetalle resolverDetalleSolicitudOp(MovimientoInventarioDTO dto,
                                                                   SolicitudMovimiento solicitud,
                                                                   LoteProducto loteOrigen) {
        if (solicitud == null) {
            return null;
        }

        List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles()).orElse(List.of());
        if (detalles.isEmpty()) {
            return null;
        }

        Long detalleId = dto != null && dto.atenciones() != null
                ? dto.atenciones().stream()
                .filter(Objects::nonNull)
                .map(AtencionDTO::getDetalleId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)
                : null;

        if (detalleId != null) {
            return solicitudMovimientoDetalleRepository.findById(detalleId)
                    .orElseGet(() -> detalles.stream()
                            .filter(det -> det != null && Objects.equals(det.getId(), detalleId))
                            .findFirst()
                            .orElse(null));
        }

        Long loteId = loteOrigen != null && loteOrigen.getId() != null
                ? loteOrigen.getId()
                : (dto != null ? dto.loteProductoId() : null);
        if (loteId == null) {
            return null;
        }

        return detalles.stream()
                .filter(Objects::nonNull)
                .filter(det -> det.getLote() != null && Objects.equals(det.getLote().getId(), loteId))
                .findFirst()
                .map(det -> det.getId() != null
                        ? solicitudMovimientoDetalleRepository.findById(det.getId()).orElse(det)
                        : det)
                .orElse(null);
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
            validarAtencionCompatibleConDetalle(solicitud.getId(), atencion, detalle);
            return detalle;
        }

        Long detalleId = resolveDetalleIdOrThrow(
                solicitud.getId(),
                atencion.getLoteId(),
                atencion.getAlmacenOrigenId() != null ? atencion.getAlmacenOrigenId().longValue() : null,
                atencion.getAlmacenDestinoId() != null ? atencion.getAlmacenDestinoId().longValue() : null,
                atencion.getCantidad());
        SolicitudMovimientoDetalle detalle = solicitudMovimientoDetalleRepository.findById(detalleId)
                .orElseThrow(() -> new NoSuchElementException("Detalle de solicitud no encontrado"));
        validarAtencionCompatibleConDetalle(solicitud.getId(), atencion, detalle);
        return detalle;
    }

    private Long resolveDetalleIdOrThrow(Long solicitudMovimientoId,
                                         Long loteId,
                                         Long almacenOrigenId,
                                         Long almacenDestinoId,
                                         BigDecimal cantidad) {
        if (solicitudMovimientoId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
        }
        if (loteId == null || almacenOrigenId == null || almacenDestinoId == null) {
            log.warn("DETALLE_SOLICITUD_MISMATCH: solicitudId={} loteId={} almacenOrigenId={} almacenDestinoId={}",
                    solicitudMovimientoId, loteId, almacenOrigenId, almacenDestinoId);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SOLICITUD_MISMATCH");
        }

        List<SolicitudMovimientoDetalle> candidatos = solicitudMovimientoDetalleRepository
                .findBySolicitudMovimientoIdAndLoteIdAndAlmacenOrigenIdAndAlmacenDestinoId(
                        solicitudMovimientoId, loteId, almacenOrigenId, almacenDestinoId);
        if (candidatos.isEmpty()) {
            Optional<String> codigoOpt = loteProductoRepository.findById(loteId)
                    .map(LoteProducto::getCodigoLote)
                    .filter(StringUtils::hasText);
            if (codigoOpt.isPresent()) {
                List<SolicitudMovimientoDetalle> candidatosCodigo = solicitudMovimientoDetalleRepository
                        .findBySolicitudMovimientoIdAndLoteCodigoAndAlmacenOrigenIdAndAlmacenDestinoId(
                                solicitudMovimientoId,
                                codigoOpt.get(),
                                almacenOrigenId,
                                almacenDestinoId);
                if (candidatosCodigo.size() == 1) {
                    SolicitudMovimientoDetalle detalle = candidatosCodigo.get(0);
                    validarCantidadDetalle(solicitudMovimientoId, detalle, cantidad);
                    log.info("DETALLE_RESUELTO_POR_CODIGO_LOTE: solicitudId={} detalleId={} loteIdOriginal={} codigoLote={}",
                            solicitudMovimientoId, detalle.getId(), loteId, codigoOpt.get());
                    return detalle.getId();
                }
                if (!candidatosCodigo.isEmpty()) {
                    log.warn("DETALLE_SOLICITUD_AMBIGUO: solicitudId={} codigoLote={} candidatos={}",
                            solicitudMovimientoId,
                            codigoOpt.get(),
                            candidatosCodigo.stream().map(SolicitudMovimientoDetalle::getId).toList());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SOLICITUD_AMBIGUO");
                }
            }
            log.warn("DETALLE_SOLICITUD_NO_ENCONTRADO: solicitudId={} loteId={} almacenOrigenId={} almacenDestinoId={}",
                    solicitudMovimientoId, loteId, almacenOrigenId, almacenDestinoId);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SOLICITUD_NO_ENCONTRADO");
        }
        if (candidatos.size() > 1) {
            log.warn("DETALLE_SOLICITUD_AMBIGUO: solicitudId={} loteId={} candidatos={}",
                    solicitudMovimientoId,
                    loteId,
                    candidatos.stream().map(SolicitudMovimientoDetalle::getId).toList());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SOLICITUD_AMBIGUO");
        }
        SolicitudMovimientoDetalle detalle = candidatos.get(0);
        validarCantidadDetalle(solicitudMovimientoId, detalle, cantidad);
        return detalle.getId();
    }

    private void validarCantidadDetalle(Long solicitudMovimientoId,
                                        SolicitudMovimientoDetalle detalle,
                                        BigDecimal cantidad) {
        BigDecimal cantidadAtencion = normalizarCantidad(cantidad);
        BigDecimal cantidadDetalle = detalle.getCantidad() != null
                ? detalle.getCantidad().setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING)
                : null;
        if (cantidadAtencion != null && cantidadDetalle != null && cantidadAtencion.compareTo(cantidadDetalle) > 0) {
            log.warn("DETALLE_SOLICITUD_MISMATCH cantidad: solicitudId={} detalleId={} detalleCantidad={} atencionCantidad={}",
                    solicitudMovimientoId, detalle.getId(), cantidadDetalle, cantidadAtencion);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DETALLE_SOLICITUD_MISMATCH");
        }
    }

    private void validarDetalleCompleto(SolicitudMovimiento solicitud, SolicitudMovimientoDetalle detalle) {
        validarDetalleCompleto(solicitud != null ? solicitud.getId() : null, detalle);
    }

    private void validarDetalleConLote(Long solicitudId, SolicitudMovimientoDetalle detalle) {
        List<String> camposFaltantes = new ArrayList<>();
        if (detalle == null) {
            camposFaltantes.add("detalle");
        } else if (detalle.getLote() == null || detalle.getLote().getId() == null) {
            camposFaltantes.add("lote_id");
        }

        if (!camposFaltantes.isEmpty()) {
            throw new CustomBusinessException(
                    ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                    "SOLICITUD_DETALLE_INCOMPLETO",
                    Map.of(
                            "solicitudId", solicitudId,
                            "detalleId", detalle != null ? detalle.getId() : null,
                            "missingFields", camposFaltantes
                    )
            );
        }
    }

    private void validarDetalleCompleto(Long solicitudId, SolicitudMovimientoDetalle detalle) {
        List<String> camposFaltantes = new ArrayList<>();
        if (detalle == null) {
            camposFaltantes.add("detalle");
        } else {
            if (detalle.getLote() == null || detalle.getLote().getId() == null) {
                camposFaltantes.add("lote_id");
            }
            if (detalle.getAlmacenOrigen() == null || detalle.getAlmacenOrigen().getId() == null) {
                camposFaltantes.add("almacen_origen_id");
            }
            if (detalle.getAlmacenDestino() == null || detalle.getAlmacenDestino().getId() == null) {
                camposFaltantes.add("almacen_destino_id");
            }
            if (detalle.getEstado() == null) {
                camposFaltantes.add("estado");
            }
        }

        if (!camposFaltantes.isEmpty()) {
            throw new CustomBusinessException(
                    ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                    "SOLICITUD_DETALLE_INCOMPLETO",
                    Map.of(
                            "solicitudId", solicitudId,
                            "detalleId", detalle != null ? detalle.getId() : null,
                            "missingFields", camposFaltantes
                    )
            );
        }
    }

    private void actualizarDetalleSolicitud(SolicitudMovimientoDetalle detalle, BigDecimal incremento) {
        BigDecimal atendidoPrevio = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO);
        BigDecimal solicitado = Optional.ofNullable(detalle.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal nuevoAtendido = atendidoPrevio.add(incremento);
        if (solicitado.compareTo(BigDecimal.ZERO) > 0 && nuevoAtendido.compareTo(solicitado) > 0) {
            nuevoAtendido = solicitado;
        }
        detalle.setCantidadAtendida(nuevoAtendido.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));

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
            EstadoSolicitudMovimiento estadoFinal = solicitud.getOrdenProduccion() != null
                    ? EstadoSolicitudMovimiento.CERRADA
                    : EstadoSolicitudMovimiento.ATENDIDA;
            solicitud.setEstado(estadoFinal);
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
    public Page<MovimientoInventarioResponseDTO> listarTodos(String codigoRecepcion, TipoMovimiento tipoMovimiento, Pageable pageable) {
        Sort sort = pageable.getSort().isEmpty()
                ? Sort.by(Sort.Direction.DESC, "fechaIngreso")
                : pageable.getSort();

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<MovimientoInventario> movimientos = repository.findAllByCodigoRecepcionAndTipoMovimiento(
                codigoRecepcion,
                tipoMovimiento,
                sortedPageable
        );
        return movimientos.map(mapper::safeToResponseDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MovimientoInventario registrarRetiroPorVencimiento(LoteProducto lote,
                                                               InventoryVencidosProperties properties,
                                                               LocalDateTime fechaMovimiento) {
        if (lote == null) {
            throw new IllegalArgumentException("El lote es requerido para registrar el retiro por vencimiento");
        }
        if (properties == null) {
            throw new IllegalArgumentException("La configuración de vencidos es requerida");
        }

        InventoryVencidosProperties.Movimiento movimientoCfg = properties.getMovimiento();
        if (!movimientoCfg.isEnabled()) {
            throw new IllegalStateException("El registro de movimientos por vencimiento está deshabilitado");
        }

        Usuario usuario;
        try {
            usuario = usuarioService.obtenerUsuarioAutenticado();
        } catch (AuthenticationCredentialsNotFoundException ex) {
            usuario = usuarioService.obtenerUsuarioSistemaJobVencimientos();
        }

        if (usuario == null) {
            throw new IllegalStateException("No se pudo resolver un usuario para registrar el movimiento por vencimiento");
        }

        Long motivoId = movimientoCfg.getMotivoId();
        MotivoMovimiento motivoMovimiento = entityManager.getReference(MotivoMovimiento.class, motivoId);
        ClasificacionMovimientoInventario clasificacion = movimientoCfg.resolveClasificacionEnum();

        Long tipoDetalleTransferenciaId = catalogResolver.getTipoDetalleTransferenciaId();
        if (tipoDetalleTransferenciaId == null) {
            throw new IllegalStateException("No se configuró el tipo de detalle de transferencia para movimientos por vencimiento");
        }
        TipoMovimientoDetalle tipoDetalle = entityManager.getReference(TipoMovimientoDetalle.class, tipoDetalleTransferenciaId);

        Almacen origen = lote.getAlmacen();
        if (origen == null || origen.getId() == null) {
            throw new IllegalStateException("El lote no tiene un almacén de origen asignado");
        }

        if (lote.getProducto() == null) {
            throw new IllegalStateException("El lote no tiene un producto asociado");
        }

        Long destinoId = properties.requireAlmacenDestinoId();
        Almacen destino = entityManager.getReference(Almacen.class, Math.toIntExact(destinoId));

        LocalDateTime fecha = fechaMovimiento != null
                ? fechaMovimiento
                : ZonedDateTime.now(properties.resolveZoneId()).toLocalDateTime();

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .cantidad(Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO))
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .clasificacion(clasificacion)
                .fechaIngreso(fecha)
                .docReferencia("JOB_VENCIDOS")
                .registradoPor(usuario)
                .producto(lote.getProducto())
                .lote(lote)
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoDetalle)
                .build();

        return repository.save(movimiento);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existeMovimientoVencimientoHoy(Long loteId,
                                                  Long motivoId,
                                                  LocalDateTime fechaInicio,
                                                  LocalDateTime fechaFin) {
        if (loteId == null || motivoId == null || fechaInicio == null || fechaFin == null) {
            return false;
        }
        return repository.existsByLoteIdAndMotivoMovimientoIdAndFechaIngresoBetween(loteId, motivoId, fechaInicio, fechaFin);
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

    @Transactional(readOnly = true)
    @Override
    public Workbook generarReporteMovimientosExcel(LocalDateTime inicio, LocalDateTime fin) {
        List<MovimientoInventario> movimientos = repository.findAllForReporte(inicio, fin);

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
                                            MotivoMovimiento motivoMovimiento,
                                            OrdenCompraDetalle ordenCompraDetalleCosto,
                                            BigDecimal gastosAdicionalesTotalRecepcion,
                                            BigDecimal subtotalTotalRecepcionConIva) {
        if (dto.loteProductoId() != null) {
            Optional<LoteProducto> existenteOpt = loteProductoRepository.findById(dto.loteProductoId());
            if (existenteOpt.isEmpty()) {
                log.warn(
                        "crearLoteRecepcion: lote no encontrado loteId={} productoId={} destinoId={} cantidad={}",
                        dto.loteProductoId(), producto.getId(),
                        destino != null ? destino.getId() : null, cantidad);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO");
            }
            LoteProducto existente = existenteOpt.get();
            BigDecimal stockAnterior = Optional.ofNullable(existente.getStockLote()).orElse(BigDecimal.ZERO);
            BigDecimal nuevo = stockAnterior.add(cantidad);
            BigDecimal costoUnitRecibo = costeoInventarioService.calcularCostoUnitarioRecepcion(
                    ordenCompraDetalleCosto, cantidad, gastosAdicionalesTotalRecepcion, subtotalTotalRecepcionConIva);
            BigDecimal costoTotalRecibo = costeoInventarioService.calcularCostoTotalLineaRecepcion(
                    ordenCompraDetalleCosto, cantidad, gastosAdicionalesTotalRecepcion, subtotalTotalRecepcionConIva);
            BigDecimal acumuladoCosto = safeScale6(existente.getCostoTotalMaterialIngresado()).add(costoTotalRecibo);
            BigDecimal totalIngresadoMaterial = safeScale6(existente.getTotalIngresadoMaterial()).add(safeScale6(cantidad));
            existente.setCostoTotalMaterialIngresado(acumuladoCosto);
            existente.setTotalIngresadoMaterial(totalIngresadoMaterial);
            if (totalIngresadoMaterial.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal nuevoCostoUnit = costeoInventarioService.calcularCostoUnitarioPromedioPorIngreso(
                        acumuladoCosto, totalIngresadoMaterial);
                existente.setCostoUnitarioMaterial(nuevoCostoUnit);
            } else {
                existente.setCostoUnitarioMaterial(costoUnitRecibo);
            }
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

        validarFechaVencimientoRecepcion(dto.fechaVencimiento());

        boolean requiereAnalisis = requiereFisico(producto) || requiereQuimico(producto) || requiereMicro(producto);
        if (requiereAnalisis) {
            // Regla de negocio: un lote con análisis requerido no puede ingresar a almacenes operativos.
            Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
            if (cuarentenaId == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_NO_CONFIGURADO");
            }
            destino = almacenRepository.findById(cuarentenaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_CUARENTENA_INEXISTENTE"));
        }

        LoteProducto lote = LoteProducto.builder()
                .codigoLote(dto.codigoLote())
                .fechaFabricacion(LocalDateTime.now())
                .fechaVencimiento(dto.fechaVencimiento())
                .fechaLiberacion(!requiereAnalisis ? LocalDateTime.now() : null)
                .estado(obtenerEstadoInicial(producto))
                .producto(producto)
                .almacen(destino)
                .usuarioLiberador(!requiereAnalisis ? usuario : null)
                .stockLote(cantidad)
                .costoTotalMaterialIngresado(costeoInventarioService.calcularCostoTotalLineaRecepcion(
                        ordenCompraDetalleCosto, cantidad, gastosAdicionalesTotalRecepcion, subtotalTotalRecepcionConIva))
                .totalIngresadoMaterial(safeScale6(cantidad))
                .build();
        if (lote.getTotalIngresadoMaterial() != null && lote.getTotalIngresadoMaterial().compareTo(BigDecimal.ZERO) > 0) {
            lote.setCostoUnitarioMaterial(costeoInventarioService.calcularCostoUnitarioPromedioPorIngreso(
                    lote.getCostoTotalMaterialIngresado(), lote.getTotalIngresadoMaterial()));
        } else {
            lote.setCostoUnitarioMaterial(costeoInventarioService.calcularCostoUnitarioRecepcion(
                    ordenCompraDetalleCosto, cantidad, gastosAdicionalesTotalRecepcion, subtotalTotalRecepcionConIva));
        }
        return loteProductoRepository.save(lote);
    }

    private void aplicarCostoMovimiento(MovimientoInventario movimiento, LoteProducto lote, BigDecimal cantidadMovimiento) {
        BigDecimal costoUnitario = lote != null ? safeScale6(lote.getCostoUnitarioMaterial()) : BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal cantidadAbs = cantidadMovimiento == null
                ? BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING)
                : cantidadMovimiento.abs().setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal costoTotal = costeoInventarioService.calcularCostoTotalMovimiento(costoUnitario, cantidadAbs);
        movimiento.setCostoUnitarioAplicado(costoUnitario);
        movimiento.setCostoTotalAplicado(costoTotal);
    }

    private OrdenCompraDetalle resolverDetalleOrdenCompra(MovimientoInventarioDTO dto, OrdenCompra orden, Producto producto) {
        if (dto.ordenCompraDetalleId() != null) {
            return entityManager.getReference(OrdenCompraDetalle.class, dto.ordenCompraDetalleId());
        }
        if (orden == null || orden.getDetalles() == null) {
            return null;
        }
        return orden.getDetalles().stream()
                .filter(det -> det.getProducto() != null && Objects.equals(det.getProducto().getId(), producto.getId()))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal calcularSubtotalLineaConIva(OrdenCompraDetalle detalle, BigDecimal cantidadRecibida) {
        if (detalle == null) {
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        BigDecimal subtotal = safeScale6(cantidadRecibida).multiply(safeScale6(detalle.getValorUnitario()));
        BigDecimal iva = subtotal.multiply(safeScale6(detalle.getIva()))
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return subtotal.add(iva).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private BigDecimal safeScale6(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private void validarFechaVencimientoRecepcion(LocalDateTime fechaVencimiento) {
        if (fechaVencimiento == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_VENCIMIENTO_REQUERIDA");
        }

        LocalDate fechaMinima = LocalDate.now().plusWeeks(1);
        LocalDate fechaLote = fechaVencimiento.toLocalDate();

        if (fechaLote.isBefore(fechaMinima)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No se puede asignar una fecha de vencimiento menor a la fecha actual más una semana");
        }
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
            MovimientoLoteDetalle detalle = consumirLoteSalidaPt(par, producto, almacenPtId, estadosElegibles, dto);
            log.info("SALIDA_PT producto={} lote={} cantidad={} autosplit={} destino={} doc={}",
                    producto != null ? producto.getId() : null,
                    detalle.lote() != null ? detalle.lote().getId() : null,
                    detalle.cantidad(),
                    autoSplitSolicitado,
                    dto.destinoTexto(),
                    dto.docReferencia());
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
                                                       EnumSet<EstadoLote> estadosElegibles,
                                                       MovimientoInventarioDTO dto) {
        if (consumo == null || consumo.loteId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_ID_REQUERIDO");
        }
        LoteProducto lote = loteProductoRepository.findByIdForUpdate(consumo.loteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO"));
        if (lote.getProducto() == null || producto.getId() == null
                || !Objects.equals(lote.getProducto().getId(), producto.getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_PRODUCTO_INVALIDO");
        }
        Long almacenActualLoteId = obtenerAlmacenActualLoteId(lote);
        if (almacenActualLoteId == null || !Objects.equals(almacenActualLoteId, almacenPtId)) {
            logLoteOrigenInvalido("salida_pt", dto, lote, almacenPtId, almacenActualLoteId,
                    dto != null ? dto.clasificacionMovimientoInventario() : null,
                    dto != null ? dto.tipoMovimiento() : null,
                    dto != null ? dto.tipoMovimientoDetalleId() : null);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
        }
        loteCalidadValidator.validarLoteUtilizable(lote);
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
                                                                           ClasificacionMovimientoInventario clasificacion,
                                                                           Almacen origen,
                                                                           Almacen destino,
                                                                           Producto producto,
                                                                           BigDecimal cantidad,
                                                                           boolean devolucionInterna,
                                                                           SolicitudMovimiento solicitud,
                                                                           AtomicBoolean solicitudOpProcesada) {
        Long loteId = resolverLoteIdSolicitud(dto, solicitud);
        if (loteId == null) {
            log.info(
                    "[INVENTARIO] movimiento con lote existente sin id de lote. tipo={} productoId={} origenId={} destinoId={} cantidad={}",
                    tipo, producto.getId(), origen != null ? origen.getId() : null,
                    destino != null ? destino.getId() : null, cantidad);
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Debe seleccionar el lote a mover.");
        }

        LoteProducto loteOrigen = loteProductoRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> {
                    log.info(
                            "[INVENTARIO] movimiento con lote inexistente. loteId={} productoId={}",
                            loteId, producto.getId());
                    return new CustomBusinessException(ApiErrorCode.RECURSO_NO_ENCONTRADO,
                            "No se encontró el lote indicado.",
                            Map.of("loteId", loteId));
                });

        boolean esPorLote = solicitud != null;
        log.debug("VAL-GATE esPorLote={} solicitudId={} tipo={}", esPorLote,
                solicitud != null ? solicitud.getId() : null, tipo);

        boolean esLoteOrigen = tipo != TipoMovimiento.ENTRADA;
        boolean esAjustePositivo = tipo == TipoMovimiento.AJUSTE
                && clasificacion == ClasificacionMovimientoInventario.AJUSTE_POSITIVO;
        boolean esAjusteNegativo = tipo == TipoMovimiento.AJUSTE
                && clasificacion == ClasificacionMovimientoInventario.AJUSTE_NEGATIVO;
        if (esLoteOrigen) {
            loteCalidadValidator.validarLoteUtilizable(loteOrigen);
        }

        Almacen almacenOrigen = origen != null
                ? origen
                : (dto.almacenOrigenId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenOrigenId())
                : null);

        boolean esDevolucionInternaCalculada =
                dto.tipoMovimiento() == TipoMovimiento.DEVOLUCION
                        && dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION;

        Long almacenActualLoteId = obtenerAlmacenActualLoteId(loteOrigen);
        if (!esDevolucionInternaCalculada
                && almacenOrigen != null
                && (almacenActualLoteId == null
                || !Objects.equals(almacenActualLoteId, almacenOrigen.getId().longValue()))) {
            log.debug("[INVENTARIO] almacén origen no coincide: loteId={} almacenLoteId={} almacenOrigenId={}",
                    loteOrigen.getId(), almacenActualLoteId, almacenOrigen.getId());
            logLoteOrigenInvalido("procesar_con_lote_existente", dto, loteOrigen,
                    almacenOrigen.getId().longValue(), almacenActualLoteId, clasificacion, tipo, dto.tipoMovimientoDetalleId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
        }

        BigDecimal stockActual = Optional.ofNullable(loteOrigen.getStockLote())
                .orElse(BigDecimal.ZERO)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado())
                .orElse(BigDecimal.ZERO)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);

        boolean esSolicitudOp = solicitud != null && solicitud.getOrdenProduccion() != null;

        boolean esOpAtencion = esSolicitudOp;

        SolicitudMovimientoDetalle detalleOp = esOpAtencion
                ? resolverDetalleSolicitudOp(dto, solicitud, loteOrigen)
                : null;
        SolicitudMovimientoDetalle detalleSolicitudRelacionado = validarDetalleCompatibleConLote(detalleOp, loteOrigen);
        boolean detalleOpGestionado = false;
        LoteProducto loteProcesadoOp = null;


        if (esOpAtencion && detalleOp != null) {
            BigDecimal solicitadaDetalle = Optional.ofNullable(detalleOp.getCantidad())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal atendida = Optional.ofNullable(detalleOp.getCantidadAtendida())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal pendienteDetalle = solicitadaDetalle.subtract(atendida);
            if (pendienteDetalle.compareTo(BigDecimal.ZERO) < 0) {
                pendienteDetalle = BigDecimal.ZERO;
            }

            if (pendienteDetalle.compareTo(BigDecimal.ZERO) <= 0) {
                Long solicitudId = detalleOp.getSolicitudMovimiento() != null
                        ? detalleOp.getSolicitudMovimiento().getId()
                        : (solicitud != null ? solicitud.getId() : null);
                log.info("[OP] Solicitud {} ya atendida para lote {}, reservaPendiente=0. Respuesta idempotente sin modificar stock.",
                        solicitudId, loteOrigen.getId());
                if (solicitudOpProcesada != null) {
                    solicitudOpProcesada.set(true);
                }
                return List.of(new MovimientoLoteDetalle(loteOrigen, BigDecimal.ZERO));
            }

            BigDecimal reservadoDisponible = reservadoActual.compareTo(BigDecimal.ZERO) > 0
                    ? reservadoActual
                    : BigDecimal.ZERO;
            if (reservadoDisponible.compareTo(pendienteDetalle) < 0) {
                log.warn("[OP] Reserva insuficiente en lote: loteId={} reservado={} requerido={} productoId={}",
                        loteOrigen.getId(), reservadoDisponible, pendienteDetalle, producto.getId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_STOCK_INSUFICIENTE");
            }

            if (stockActual.compareTo(pendienteDetalle) < 0) {
                log.warn("Stock físico insuficiente en lote (OP): loteId={} stockLote={} solicitado={} productoId={}",
                        loteOrigen.getId(), stockActual, pendienteDetalle, producto.getId());
                throw loteStockInsuficienteException(loteOrigen, producto, pendienteDetalle, stockActual, loteOrigen.getAlmacen());
            }

            reservaLoteService.consumirReserva(solicitud, detalleOp, loteOrigen, pendienteDetalle);

            BigDecimal stockAntes = stockActual;
            BigDecimal reservadoAntes = reservadoActual;
            log.debug("VAL-ACTUALIZA (OP) antes actualizarStockLote loteId={} stockAntes={} reservadoAntes={} req={}",
                    loteOrigen.getId(), stockAntes, reservadoAntes, pendienteDetalle);

            BigDecimal nuevoStock = stockActual.subtract(pendienteDetalle);
            if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
                throw loteStockInsuficienteException(loteOrigen, producto, pendienteDetalle, stockActual, loteOrigen.getAlmacen());
            }

            BigDecimal nuevoReservado = reservadoActual.subtract(pendienteDetalle);
            if (nuevoReservado.compareTo(BigDecimal.ZERO) < 0) {
                nuevoReservado = BigDecimal.ZERO;
            }

            int escala = resolverEscalaProducto(producto);
            loteOrigen.setStockLote(nuevoStock.setScale(escala, RoundingMode.HALF_UP));
            loteOrigen.setStockReservado(nuevoReservado.setScale(escala, RoundingMode.HALF_UP));
            if (loteOrigen.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
                loteOrigen.setAgotado(true);
                if (loteOrigen.getFechaAgotado() == null) {
                    loteOrigen.setFechaAgotado(LocalDateTime.now());
                }
            } else {
                loteOrigen.setAgotado(false);
                loteOrigen.setFechaAgotado(null);
            }

            loteProcesadoOp = loteProductoRepository.save(loteOrigen);

            actualizarDetalleSolicitud(detalleOp, pendienteDetalle);
            solicitudMovimientoDetalleRepository.save(detalleOp);

            actualizarEstadoSolicitud(solicitud);
            solicitudMovimientoRepository.saveAndFlush(solicitud);

            detalleOpGestionado = true;
            if (solicitudOpProcesada != null) {
                solicitudOpProcesada.set(true);
            }

            stockActual = Optional.ofNullable(loteOrigen.getStockLote())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado())
                    .orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            cantidad = pendienteDetalle;
        }

        if (detalleSolicitudRelacionado == null && solicitud != null) {
            SolicitudMovimientoDetalle posibleDetalle = resolverDetalleSolicitudOp(dto, solicitud, loteOrigen);
            detalleSolicitudRelacionado = validarDetalleCompatibleConLote(posibleDetalle, loteOrigen);
        }

        BigDecimal reservaPendiente = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        if (solicitud != null && loteOrigen != null && loteOrigen.getId() != null) {
            BigDecimal reservaDetalle = calcularReservaPendienteParaDetalle(solicitud, detalleSolicitudRelacionado, loteOrigen)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            if (reservaDetalle.compareTo(BigDecimal.ZERO) > 0) {
                reservaPendiente = reservaDetalle;
            } else {
                BigDecimal reservaDb = Optional.ofNullable(
                                solicitudMovimientoDetalleRepository.calcularReservaPendientePorSolicitudYLote(
                                        solicitud.getId(), loteOrigen.getId()))
                        .orElse(BigDecimal.ZERO)
                        .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
                BigDecimal reservadoPositivo = reservadoActual.compareTo(BigDecimal.ZERO) > 0
                        ? reservadoActual
                        : BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
                reservaPendiente = reservaDb.min(reservadoPositivo).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            }
            log.debug("VAL-LOTE-RESERVA solicitudId={} loteId={} estadoSolicitud={} stockLote={} stockReservado={} reservaPendiente={}",
                    solicitud.getId(), loteOrigen.getId(), solicitud.getEstado(), stockActual, reservadoActual, reservaPendiente);
        }

        BigDecimal stockDisponibleBase = stockActual.subtract(reservadoActual)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal stockDisponibleEfectivo = stockDisponibleBase.add(reservaPendiente)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);

        SolicitudMovimiento solicitudContexto = solicitud != null
                ? solicitud
                : (detalleSolicitudRelacionado != null ? detalleSolicitudRelacionado.getSolicitudMovimiento() : null);
        SolicitudMovimientoDetalle detalleContexto = detalleSolicitudRelacionado;

        BigDecimal stockReservadoTotal = nvl(loteOrigen.getStockReservado()).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal reservadoPropio = obtenerCantidadReservadaPorEsteDetalle(detalleContexto);
        if (reservadoPropio.compareTo(stockReservadoTotal) > 0) {
            reservadoPropio = stockReservadoTotal;
        }
        BigDecimal reservaAjena = stockReservadoTotal.subtract(reservadoPropio);
        if (reservaAjena.compareTo(BigDecimal.ZERO) < 0) {
            reservaAjena = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        BigDecimal stockDisponibleEfectivoContextual = stockActual.subtract(reservaAjena)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal cantidadSolicitadaContextual = obtenerCantidadSolicitadaDelDetalle(detalleContexto, cantidad);

        log.debug("VAL-LOTE loteId={} estadoSolicitud={} stockLote={} reservadoTotal={} pendienteSolicitud={} disponibleBase={}disponibleEfectivo={} req={}",
                loteOrigen.getId(), solicitud != null ? solicitud.getEstado() : null, stockActual, reservadoActual,
                reservaPendiente, stockDisponibleBase, stockDisponibleEfectivo, cantidad);

        boolean hayContextoSolicitud = solicitudContexto != null || detalleContexto != null;

        boolean esTransferenciaInternaProduccion = tipo == TipoMovimiento.TRANSFERENCIA
                && clasificacion == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
        boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());
        boolean requiereAutoSplit = tipo == TipoMovimiento.TRANSFERENCIA
                && esTransferenciaInternaProduccion
                && autoSplitSolicitado
                && cantidad.compareTo(stockDisponibleBase) > 0;

        if (EnumSet.of(TipoMovimiento.SALIDA, TipoMovimiento.TRANSFERENCIA,
                TipoMovimiento.DEVOLUCION, TipoMovimiento.AJUSTE).contains(tipo) && !esAjustePositivo) {
            if (detalleOpGestionado) {
                // Ya se consumió la reserva del detalle OP en esta misma ejecución.
            } else if (solicitud != null
                    && solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA
                    && !requiereAutoSplit) {
                if (reservadoActual.compareTo(cantidad) < 0) {
                    log.warn("RESERVA_INSUFICIENTE: loteId={} reservado={} solicitado={} productoId={}",
                            loteOrigen.getId(), reservadoActual, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_INSUFICIENTE");
                }
            } else if (!requiereAutoSplit) {
                log.debug("VAL-LOTE-CONTEXTO solicitudIdDto={} solicitudCtxId={} detalleCtxId={} stockLote={} reservadoTotal={} reservadoPropio={} reservaAjena={} disponibleCtx={} solicitadoCtx={}",
                        dto.solicitudMovimientoId(),
                        solicitudContexto != null ? solicitudContexto.getId() : null,
                        detalleContexto != null ? detalleContexto.getId() : null,
                        stockActual, stockReservadoTotal, reservadoPropio, reservaAjena, stockDisponibleEfectivoContextual,
                        cantidadSolicitadaContextual);

                if (hayContextoSolicitud) {
                    if (stockDisponibleEfectivoContextual.compareTo(cantidadSolicitadaContextual) < 0) {
                        log.warn(
                                "Stock insuficiente en lote (CON_SOLICITUD): solicitudId={} detalleId={} loteId={} disponible={} reservadoTotal={} reservadoOtros={} reservadoDetalle={} solicitado={} productoId={}",
                                solicitudContexto != null ? solicitudContexto.getId() : null,
                                detalleContexto != null ? detalleContexto.getId() : null,
                                loteOrigen.getId(), stockDisponibleEfectivoContextual, stockReservadoTotal,
                                reservaAjena, reservadoPropio, cantidadSolicitadaContextual, producto.getId());
                        if (tipo == TipoMovimiento.TRANSFERENCIA) {
                            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_DISPONIBLE_TRANSFERIR");
                        }
                        throw loteStockInsuficienteException(loteOrigen, producto, cantidadSolicitadaContextual, stockDisponibleEfectivoContextual, loteOrigen.getAlmacen());
                    }
                } else if (stockDisponibleEfectivo.compareTo(cantidad) < 0) {
                    log.warn("Stock insuficiente en lote (SIN_SOLICITUD): loteId={} disponible={} reservaPendiente={} solicitado={} productoId={}",
                            loteOrigen.getId(), stockDisponibleEfectivo, reservaPendiente, cantidad, producto.getId());
                    if (tipo == TipoMovimiento.TRANSFERENCIA) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_DISPONIBLE_TRANSFERIR");
                    }
                    throw loteStockInsuficienteException(loteOrigen, producto, cantidad, stockDisponibleEfectivo, loteOrigen.getAlmacen());
                }
            }
        }

        if (tipo == TipoMovimiento.SALIDA) {
            log.debug("MOV-SALIDA procesando prod={}, qty={}, solicitudId={}, opId={}",
                    dto.productoId(), cantidad, solicitud != null ? solicitud.getId() : null, dto.ordenProduccionId());
            if (solicitud != null) {
                if (solicitudOpProcesada != null && solicitudOpProcesada.get()) {
                    LoteProducto loteRespuesta = loteProcesadoOp != null ? loteProcesadoOp : loteOrigen;
                    return List.of(new MovimientoLoteDetalle(loteRespuesta, cantidad));
                }
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

        if (tipo == TipoMovimiento.AJUSTE) {
            if (esAjusteNegativo) {
                log.debug("VAL-ACTUALIZA (AJUSTE-) antes actualizarStockLote loteId={} stockAntes={} req={}",
                        loteOrigen.getId(), loteOrigen.getStockLote(), cantidad);
                actualizarStockLote(loteOrigen, cantidad, producto);
                LoteProducto actualizado = loteProductoRepository.save(loteOrigen);
                return List.of(new MovimientoLoteDetalle(actualizado, cantidad));
            }
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
            if (detalleOpGestionado) {
                if (destino == null) {
                    log.warn("TRANSFERENCIA_DESTINO_REQUERIDO_OP loteId={} productoId={} cantidad={}",
                            loteOrigen.getId(), producto.getId(), cantidad);
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_REQUERIDO");
                }
                LoteProducto loteDestino = acreditarTransferenciaEnDestino(loteOrigen, destino, producto, cantidad);
                return List.of(new MovimientoLoteDetalle(loteDestino, cantidad));
            }
            // Se rechaza la transferencia solo si el lote está agotado/sin disponible, en otro almacén de origen
            // o su estado no es transferible (calidad ya se valida en LoteCalidadValidator).
            Long almacenActualId = loteOrigen.getAlmacen() != null
                    ? loteOrigen.getAlmacen().getId().longValue()
                    : null;
            Long almacenOrigenSolicitudId = almacenOrigen != null
                    ? almacenOrigen.getId().longValue()
                    : (dto.almacenOrigenId() != null ? dto.almacenOrigenId().longValue() : null);
            boolean almacenesCoinciden = almacenOrigenSolicitudId == null
                    || Objects.equals(almacenActualId, almacenOrigenSolicitudId);

            BigDecimal disponibleTransferencia = calcularDisponibleLote(loteOrigen);
            boolean sinDisponible = loteOrigen.isAgotado()
                    || disponibleTransferencia.compareTo(cantidad) < 0;

            boolean estadoTransferible = loteOrigen.getEstado() != null
                    && EnumSet.of(EstadoLote.DISPONIBLE, EstadoLote.LIBERADO).contains(loteOrigen.getEstado());

            // BUG: con el nuevo validador de calidad se seguía exigiendo DISPONIBLE y se rechazaban lotes LIBERADO.
            if (sinDisponible || !almacenesCoinciden || !estadoTransferible) {
                log.warn(
                        "Transferencia con lote no disponible (almacen/stock/estado inválido): loteId={} estado={} agotado={} disponible={} solicitado={} almacenActualId={} almacenOrigenSolicitudId={} destinoId={} productoId={}",
                        loteOrigen.getId(), loteOrigen.getEstado(), loteOrigen.isAgotado(), disponibleTransferencia,
                        cantidad, almacenActualId, almacenOrigenSolicitudId, destino != null ? destino.getId() : null,
                        producto.getId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "LOTE_NO_DISPONIBLE_TRANSFERIR");
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

            BigDecimal cantidadInicial = cantidad.min(stockDisponibleEfectivo);
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
                    Long almacenActualLoteAdicionalId = obtenerAlmacenActualLoteId(loteAdicional);
                    if (almacenActualLoteAdicionalId == null
                            || !Objects.equals(almacenActualLoteAdicionalId, almacenOrigenId.longValue())) {
                        log.warn("AUTO_SPLIT_ALMACEN_INCONSISTENTE loteId={} almacenEsperado={} almacenEncontrado={}",
                                loteAdicional.getId(), almacenOrigenId, almacenActualLoteAdicionalId);
                        logLoteOrigenInvalido("auto_split", dto, loteAdicional, almacenOrigenId.longValue(),
                                almacenActualLoteAdicionalId, clasificacion, tipo, dto.tipoMovimientoDetalleId());
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_PERTENECE_ALMACEN_ORIGEN");
                    }

                    BigDecimal disponibleActual = calcularDisponibleLote(loteAdicional);
                    if (disponibleActual.compareTo(par.cantidad()) < 0) {
                        log.warn("AUTO_SPLIT_DISPONIBLE_INSUFICIENTE loteId={} disponible={} requerido={}",
                                loteAdicional.getId(), disponibleActual, par.cantidad());
                        throw new CustomBusinessException(ApiErrorCode.STOCK_INSUFICIENTE,
                                "Stock insuficiente en lote FEFO seleccionado",
                                Map.of(
                                        "loteId", loteAdicional.getId(),
                                        "disponible", disponibleActual,
                                        "requerido", par.cantidad()
                                ));
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

    private List<MovimientoLoteDetalle> procesarRecepcionDevolucionCliente(MovimientoInventarioDTO dto,
                                                                           Producto producto,
                                                                           BigDecimal cantidad,
                                                                           Almacen almacenDestino,
                                                                           boolean loteLegacy) {
        if (almacenDestino == null) {
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS,
                    "Debe indicar un almacén destino para la devolución",
                    Map.of("productoId", dto.productoId()));
        }

        LoteProducto loteDestino;
        if (!loteLegacy) {
            Long loteId = dto.loteProductoId();
            LoteProducto loteOrigen = loteProductoRepository.findByIdForUpdate(loteId)
                    .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_LOTE_REQUERIDO,
                            "No se encontró el lote para la devolución",
                            Map.of("loteId", loteId)));

            if (loteOrigen.getFechaVencimiento() == null) {
                if (dto.fechaVencimiento() == null) {
                    throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_REQUERIDA,
                            "Debe indicar fecha de vencimiento para la devolución",
                            Map.of("loteId", loteId));
                }
                loteOrigen.setFechaVencimiento(dto.fechaVencimiento());
                loteOrigen = loteProductoRepository.save(loteOrigen);
            }
            if (loteOrigen.getFechaVencimiento() != null && dto.fechaVencimiento() != null
                    && !mismaFechaCalendario(loteOrigen.getFechaVencimiento(), dto.fechaVencimiento())) {
                throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_INVALIDA,
                        "La fecha de vencimiento enviada no coincide con la del lote origen",
                        Map.of(
                                "loteId", loteId,
                                "fechaVencimientoLote", loteOrigen.getFechaVencimiento(),
                                "fechaVencimientoPayload", dto.fechaVencimiento()
                        ));
            }

            if (loteOrigen.getProducto() == null || !Objects.equals(loteOrigen.getProducto().getId(), producto.getId())) {
                throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_COMBINACION_INVALIDA,
                        "El lote no corresponde al producto de la devolución",
                        Map.of("loteId", loteId, "productoId", producto.getId()));
            }

            String codigoLote = loteOrigen.getCodigoLote();
            if (!StringUtils.hasText(codigoLote)) {
                throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS,
                        "El lote no tiene código para registrar la devolución",
                        Map.of("loteId", loteId));
            }

            // TODO: validar que el lote tenga al menos una SALIDA_CLIENTE previa.
            loteDestino = ensureDestinoLote(producto, codigoLote, loteOrigen, almacenDestino, dto.fechaVencimiento());
        } else {
            if (dto.fechaVencimiento() == null) {
                throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_REQUERIDA,
                        "Debe indicar fecha de vencimiento para la devolución legacy",
                        Map.of("codigoLote", dto.codigoLote()));
            }

            String codigoLote = dto.codigoLote().trim();
            Optional<LoteProducto> destinoExistente = loteProductoRepository
                    .findByCodigoLoteAndProductoIdAndAlmacenId(
                            codigoLote,
                            producto.getId(),
                            almacenDestino.getId());

            EstadoLote estadoLegacy = dto.condicionProductoDevuelto() == CondicionProductoDevuelto.OPTIMO
                    ? EstadoLote.LIBERADO
                    : EstadoLote.EN_CUARENTENA;

            if (destinoExistente.isPresent()) {
                loteDestino = destinoExistente.get();
                if (loteDestino.getEstado() == null) {
                    loteDestino.setEstado(estadoLegacy);
                }
                validarYAsignarFechaVencimientoDevolucion(loteDestino, dto.fechaVencimiento());
            } else {
                loteDestino = new LoteProducto();
                loteDestino.setProducto(producto);
                loteDestino.setCodigoLote(codigoLote);
                loteDestino.setAlmacen(almacenDestino);
                loteDestino.setEstado(estadoLegacy);
                loteDestino.setStockLote(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
                loteDestino.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
                loteDestino.setFechaVencimiento(dto.fechaVencimiento());
            }
        }

        if (catalogResolver.getAlmacenCuarentenaId() != null
                && Objects.equals(almacenDestino.getId().longValue(), catalogResolver.getAlmacenCuarentenaId())) {
            loteDestino.setEstado(EstadoLote.EN_CUARENTENA);
        }

        LoteProducto actualizado = acreditarRecepcionDevolucion(loteDestino, cantidad, producto);
        return List.of(new MovimientoLoteDetalle(actualizado, cantidad));
    }

    private LoteProducto acreditarRecepcionDevolucion(LoteProducto loteDestino,
                                                      BigDecimal cantidad,
                                                      Producto producto) {
        int escala = resolverEscalaProducto(producto);
        BigDecimal stockActual = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal nuevoStock = stockActual.add(cantidad).setScale(escala, RoundingMode.HALF_UP);
        loteDestino.setStockLote(nuevoStock);
        if (loteDestino.getStockReservado() == null) {
            loteDestino.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        }
        recalcularAgotadoSegunDisponibilidad(loteDestino);
        return loteProductoRepository.save(loteDestino);
    }

    private void validarRecepcionDevolucionCliente(MovimientoInventarioDTO dto) {
        boolean faltanDatos = !StringUtils.hasText(dto.clienteNombre())
                || !StringUtils.hasText(dto.docReferencia())
                || dto.causaDevolucionPt() == null
                || dto.condicionProductoDevuelto() == null
                || dto.cantidad() == null
                || dto.cantidad().compareTo(BigDecimal.ZERO) <= 0;

        if (faltanDatos) {
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("clienteNombre", dto.clienteNombre());
            detalles.put("docReferencia", dto.docReferencia());
            detalles.put("causaDevolucionPt", dto.causaDevolucionPt());
            detalles.put("condicionProductoDevuelto", dto.condicionProductoDevuelto());
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS,
                    "Faltan datos obligatorios para registrar la devolución del cliente",
                    detalles);
        }

        if (Boolean.TRUE.equals(dto.loteLegacy()) && !StringUtils.hasText(dto.destinoTexto())) {
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("observaciones", dto.destinoTexto());
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_LEGACY_REQUIERE_OBSERVACIONES,
                    "La devolución legacy requiere observaciones adicionales",
                    detalles);
        }

        if (Boolean.TRUE.equals(dto.loteLegacy()) && !StringUtils.hasText(dto.codigoLote())) {
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("codigoLote", dto.codigoLote());
            throw new CustomBusinessException(ApiErrorCode.CODIGO_LOTE_LEGACY_REQUERIDO,
                    "Debe indicar el código de lote legacy",
                    detalles);
        }
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
                .map(c -> c.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING))
                .orElse(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        if (cantidadNormalizada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_TRANSFERENCIA_INVALIDA");
        }

        BigDecimal stockActual = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO);
        if (stockActual.compareTo(cantidadNormalizada) < 0) {
            log.warn("TRANSFERENCIA_STOCK_INSUFICIENTE loteId={} stockActual={} requerido={}",
                    loteOrigen.getId(), stockActual, cantidadNormalizada);
            throw loteStockInsuficienteException(loteOrigen, producto, cantidadNormalizada, stockActual, loteOrigen.getAlmacen());
        }

        LoteProducto loteDestino = ensureDestinoLote(producto, loteOrigen.getCodigoLote(), loteOrigen, destino, null);

        int escala = resolverEscalaProducto(producto);
        BigDecimal nuevoStockOrigen = stockActual.subtract(cantidadNormalizada)
                .setScale(escala, RoundingMode.HALF_UP);
        loteOrigen.setStockLote(nuevoStockOrigen);

        if (esAtencionReserva(solicitud)) {
            BigDecimal reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(BigDecimal.ZERO);
            SolicitudMovimientoDetalle detalleReserva = resolverDetalleSolicitudOp(null, solicitud, loteOrigen);
            detalleReserva = validarDetalleCompatibleConLote(detalleReserva, loteOrigen);
            BigDecimal reservadoPorDetalle = obtenerCantidadReservadaPorEsteDetalle(detalleReserva);
            if (reservadoPorDetalle.compareTo(reservadoActual) > 0) {
                reservadoPorDetalle = reservadoActual;
            }
            BigDecimal decremento = reservadoActual.min(cantidadNormalizada);
            if (reservadoPorDetalle.compareTo(BigDecimal.ZERO) > 0) {
                decremento = reservadoPorDetalle.min(decremento);
            }
            BigDecimal nuevoReservado = reservadoActual.subtract(decremento);
            if (nuevoReservado.compareTo(BigDecimal.ZERO) < 0) {
                nuevoReservado = BigDecimal.ZERO;
            }
            loteOrigen.setStockReservado(nuevoReservado.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        }

        recalcularAgotadoSegunDisponibilidad(loteOrigen);
        loteProductoRepository.save(loteOrigen);

        BigDecimal stockDestino = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal nuevoStockDestino = stockDestino.add(cantidadNormalizada)
                .setScale(escala, RoundingMode.HALF_UP);
        loteDestino.setStockLote(nuevoStockDestino);
        if (loteDestino.getStockReservado() == null) {
            loteDestino.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        }
        if (destino.getCategoria() == TipoCategoria.OBSOLETOS) {
            loteDestino.setEstado(EstadoLote.RECHAZADO);
        } else if (loteDestino.getEstado() == null) {
            loteDestino.setEstado(obtenerEstadoInicial(producto));
        }

        recalcularAgotadoSegunDisponibilidad(loteDestino);
        LoteProducto guardadoDestino = loteProductoRepository.save(loteDestino);
        BigDecimal cantidadDetalle = cantidadNormalizada.setScale(escala, RoundingMode.HALF_UP);
        return new MovimientoLoteDetalle(guardadoDestino, cantidadDetalle);
    }

    private LoteProducto acreditarTransferenciaEnDestino(LoteProducto loteOrigen,
                                                         Almacen destino,
                                                         Producto producto,
                                                         BigDecimal cantidadTransferida) {
        LoteProducto loteDestino = ensureDestinoLote(producto, loteOrigen.getCodigoLote(), loteOrigen, destino, null);
        int escala = resolverEscalaProducto(producto);
        BigDecimal stockDestino = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO)
                .setScale(escala, RoundingMode.HALF_UP);
        BigDecimal nuevoStockDestino = stockDestino.add(cantidadTransferida.setScale(escala, RoundingMode.HALF_UP));
        loteDestino.setStockLote(nuevoStockDestino);
        if (loteDestino.getStockReservado() == null) {
            loteDestino.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        }
        if (loteDestino.getLoteOrigen() == null) {
            loteDestino.setLoteOrigen(loteOrigen);
        }
        recalcularAgotadoSegunDisponibilidad(loteDestino);
        return loteProductoRepository.save(loteDestino);
    }

    private LoteProducto ensureDestinoLote(
            Producto producto,
            String codigoLote,
            LoteProducto loteOrigen,
            Almacen almacenDestino,
            LocalDateTime fechaVencimientoEsperada
    ) {
        Integer prodId = producto.getId();
        Integer destinoId = almacenDestino.getId();

        // 1) Intentar encontrar el lote EXACTO en el almacén destino
        Optional<LoteProducto> exacto = loteProductoRepository
                .findByCodigoLoteAndProductoIdAndAlmacenId(codigoLote, prodId, destinoId);

        if (exacto.isPresent()) {
            LoteProducto existente = exacto.get();
            boolean modificado = false;
            if (existente.getLoteOrigen() == null && loteOrigen != null) {
                existente.setLoteOrigen(loteOrigen);
                modificado = true;
            }
            LocalDateTime fechaAntes = existente.getFechaVencimiento();
            validarYAsignarFechaVencimientoDevolucion(existente, fechaVencimientoEsperada);
            if (!Objects.equals(fechaAntes, existente.getFechaVencimiento())) {
                modificado = true;
            }
            if (modificado) {
                loteProductoRepository.save(existente);
            }
            return existente;
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
            nuevo.setLoteOrigen(loteOrigen);
            // NO copiar stock: se ajustará por el movimiento
            nuevo.setStockLote(BigDecimal.ZERO);
            nuevo.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        } else {
            nuevo.setStockLote(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
            nuevo.setStockReservado(BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING));
        }
        validarYAsignarFechaVencimientoDevolucion(nuevo, fechaVencimientoEsperada);

        LoteProducto guardado = loteProductoRepository.save(nuevo);
        log.info("DESTINO_LOTE_CREADO: code={}, prod={}, destinoId={}, loteId={}",
                codigoLote, prodId, destinoId, guardado.getId());
        return guardado;
    }


    private void validarYAsignarFechaVencimientoDevolucion(LoteProducto lote, LocalDateTime fechaVencimientoEsperada) {
        if (fechaVencimientoEsperada == null) {
            return;
        }
        if (lote.getFechaVencimiento() == null) {
            lote.setFechaVencimiento(fechaVencimientoEsperada);
            return;
        }
        if (!mismaFechaCalendario(lote.getFechaVencimiento(), fechaVencimientoEsperada)) {
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_FECHA_VENCIMIENTO_INVALIDA,
                    "La fecha de vencimiento enviada no coincide con la del lote",
                    Map.of(
                            "loteId", lote.getId(),
                            "fechaVencimientoLote", lote.getFechaVencimiento(),
                            "fechaVencimientoPayload", fechaVencimientoEsperada
                    ));
        }
    }


    private boolean mismaFechaCalendario(LocalDateTime fechaLote, LocalDateTime fechaPayload) {
        if (fechaLote == null || fechaPayload == null) {
            return Objects.equals(fechaLote, fechaPayload);
        }
        return fechaLote.toLocalDate().equals(fechaPayload.toLocalDate());
    }

    private void recalcularAgotadoSegunDisponibilidad(LoteProducto lote) {
        BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal reservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO)
                .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        List<SolicitudMovimientoDetalle> detalles = solicitud.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }

        Long loteId = lote.getId();
        BigDecimal total = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal atendida = Optional.ofNullable(detalle.getCantidadAtendida()).orElse(BigDecimal.ZERO)
                    .setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            BigDecimal pendiente = cantidadDetalle.subtract(atendida);
            if (pendiente.compareTo(BigDecimal.ZERO) < 0) {
                pendiente = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            }
            total = total.add(pendiente);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        return total.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
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

        Long almacenIdLong = almacenOrigenId != null ? almacenOrigenId.longValue() : null;
        Collection<Long> excluidos = loteInicialId != null
                ? Set.of(loteInicialId)
                : Collections.emptySet();

        List<FefoSelection> selecciones = calcularSeleccionesFefo(
                productoId,
                objetivo,
                almacenIdLong,
                excluidos);

        return selecciones.stream()
                .map(sel -> new ParLoteCantidad(sel.lote().getId(), sel.tomar()))
                .toList();
    }

    private Long resolveTipoMovimientoDetalleId(MovimientoInventarioDTO dto, Producto producto) {
        if (dto.tipoMovimientoDetalleId() != null) {
            return dto.tipoMovimientoDetalleId();
        }
        if (dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.RECEPCION_DEVOLUCION_CLIENTE) {
            return resolveTipoDetalleRecepcionDevolucionCliente(dto);
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

    private Long resolveTipoDetalleRecepcionDevolucionCliente(MovimientoInventarioDTO dto) {
        boolean vaABodegaPt = dto.condicionProductoDevuelto() == CondicionProductoDevuelto.OPTIMO;
        Long tipoDetalleTransferenciaId = catalogResolver.getTipoDetalleTransferenciaId();
        Long tipoDetalleEntradaId = catalogResolver.getTipoDetalleEntradaId();
        if (!vaABodegaPt && tipoDetalleTransferenciaId != null) {
            return tipoDetalleTransferenciaId;
        }
        if (tipoDetalleEntradaId == null) {
            String destino = vaABodegaPt ? "BODEGA_PT" : "CUARENTENA";
            throw new CustomBusinessException(
                    ApiErrorCode.CATALOGO_TIPO_DETALLE_ENTRADA_FALTANTE,
                    "No se encontró el tipo detalle de entrada para la devolución de cliente",
                    Map.of("destino", destino));
        }
        return tipoDetalleEntradaId;
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
        if (!catalogResolver.isSalidaPtEnabled() || tipoMovimiento != TipoMovimiento.SALIDA || tipoDetalleId == null) {
            return false;
        }
        Long salidaPtId = catalogResolver.getTipoDetalleSalidaPtId();
        if (salidaPtId != null) {
            return Objects.equals(tipoDetalleId, salidaPtId);
        }
        Long salidaId = catalogResolver.getTipoDetalleSalidaId();
        return salidaId != null && Objects.equals(tipoDetalleId, salidaId);
    }

    private void logLoteOrigenInvalido(String origenLog,
                                       MovimientoInventarioDTO dto,
                                       LoteProducto lote,
                                       Long almacenEsperadoId,
                                       Long almacenActualLoteId,
                                       ClasificacionMovimientoInventario clasificacion,
                                       TipoMovimiento tipoMovimiento,
                                       Long tipoDetalleId) {
        log.warn("LOTE_NO_PERTENECE_ALMACEN_ORIGEN traceId={} origen={} opId={} etapaId={} productoId={} loteId={} codigoLote={} dtoAlmacenOrigenId={} dtoAlmacenDestinoId={} almacenEsperadoId={} loteAlmacenActualId={} loteOrigenId={} loteOrigenAlmacenId={} clasificacion={} tipoMovimiento={} tipoDetalleId={}",
                MDC.get("opTraceId"),
                origenLog,
                dto != null ? dto.ordenProduccionId() : null,
                dto != null ? dto.ordenProduccionEtapaId() : null,
                dto != null ? dto.productoId() : (lote != null && lote.getProducto() != null ? lote.getProducto().getId() : null),
                lote != null ? lote.getId() : null,
                lote != null ? lote.getCodigoLote() : null,
                dto != null ? dto.almacenOrigenId() : null,
                dto != null ? dto.almacenDestinoId() : null,
                almacenEsperadoId,
                almacenActualLoteId,
                lote != null && lote.getLoteOrigen() != null ? lote.getLoteOrigen().getId() : null,
                lote != null && lote.getLoteOrigen() != null && lote.getLoteOrigen().getAlmacen() != null ? lote.getLoteOrigen().getAlmacen().getId() : null,
                clasificacion,
                tipoMovimiento,
                tipoDetalleId
        );
    }

    private MotivoMovimiento resolverMotivoMovimientoPorClasificacion(
            ClasificacionMovimientoInventario clasificacion,
            MotivoMovimiento motivoActual
    ) {
        if (motivoActual != null) {
            return motivoActual;
        }
        if (clasificacion == ClasificacionMovimientoInventario.SALIDA_CLIENTE) {
            return motivoMovimientoRepository.findByMotivo(ClasificacionMovimientoInventario.SALIDA_CLIENTE)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró MotivoMovimiento configurado para SALIDA_CLIENTE"));
        }
        return null;
    }

    private void validarUbicacionDestinoTransferenciaPt(MovimientoInventarioDTO dto,
                                                       TipoMovimiento tipoMovimiento,
                                                       ClasificacionMovimientoInventario clasificacion,
                                                       Almacen almacenOrigen,
                                                       Almacen almacenDestino) {
        if (dto == null
                || tipoMovimiento != TipoMovimiento.TRANSFERENCIA
                || clasificacion != ClasificacionMovimientoInventario.TRANSFERENCIA_GENERAL
                || dto.ubicacionDestinoId() != null
                || almacenOrigen == null
                || almacenDestino == null
                || !esTransferenciaCuarentenaABodegaPt(almacenOrigen, almacenDestino)) {
            return;
        }

        Integer almacenDestinoId = almacenDestino.getId();
        if (almacenDestinoId != null && ubicacionFisicaRepository.existsByAlmacenIdAndActivoTrue(almacenDestinoId)) {
            throw new CustomBusinessException(ApiErrorCode.UBICACION_DESTINO_REQUERIDA,
                    "Debe indicar ubicación destino para la transferencia a Bodega PT",
                    Map.of("almacenDestinoId", almacenDestinoId));
        }
    }

    private boolean esTransferenciaCuarentenaABodegaPt(Almacen almacenOrigen, Almacen almacenDestino) {
        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (almacenCuarentenaId == null || almacenOrigen.getId() == null || almacenDestino.getId() == null) {
            return false;
        }

        if (!Objects.equals(almacenOrigen.getId().longValue(), almacenCuarentenaId)) {
            return false;
        }

        if (almacenDestino.getCategoria() == TipoCategoria.PRODUCTO_TERMINADO) {
            return true;
        }

        Long almacenPtId = catalogResolver.getAlmacenPtId();
        return almacenPtId != null && Objects.equals(almacenDestino.getId().longValue(), almacenPtId);
    }

    private Long resolverMotivoDevolucionCliente(Almacen almacenDestino) {
        if (almacenDestino == null) {
            throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_DATOS_INCOMPLETOS,
                    "Debe indicar un almacén destino para la devolución",
                    Map.of());
        }

        Long destinoId = almacenDestino.getId() != null ? almacenDestino.getId().longValue() : null;
        Long almacenPtId = catalogResolver.getAlmacenPtId();
        Long almacenCuarentenaId = catalogResolver.getAlmacenCuarentenaId();

        if (destinoId != null && Objects.equals(destinoId, almacenPtId)) {
            Long motivoId = catalogResolver.getMotivoIdEntradaProductoTerminado();
            if (motivoId == null) {
                throw new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE,
                        "No se encontró el motivo de entrada para producto terminado",
                        Map.of("almacenDestinoId", destinoId));
            }
            return motivoId;
        }

        if (destinoId != null && Objects.equals(destinoId, almacenCuarentenaId)) {
            Long motivoId = catalogResolver.getMotivoIdTransferenciaCalidad();
            if (motivoId == null) {
                throw new CustomBusinessException(ApiErrorCode.CATALOGO_FALTANTE,
                        "No se encontró el motivo de transferencia a calidad",
                        Map.of("almacenDestinoId", destinoId));
            }
            return motivoId;
        }

        throw new CustomBusinessException(ApiErrorCode.DEVOLUCION_PT_COMBINACION_INVALIDA,
                "El almacén destino no es válido para la devolución de cliente",
                Map.of("almacenDestinoId", destinoId));
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
        copia.setCausaDevolucionPt(base.getCausaDevolucionPt());
        copia.setCondicionProductoDevuelto(base.getCondicionProductoDevuelto());
        copia.setClienteNombre(base.getClienteNombre());
        copia.setLoteLegacy(base.isLoteLegacy());
        copia.setRegistradoPor(base.getRegistradoPor());
        copia.setProducto(base.getProducto());
        copia.setLote(lote);
        copia.setAlmacenOrigen(base.getAlmacenOrigen());
        copia.setAlmacenDestino(base.getAlmacenDestino());
        copia.setProveedor(base.getProveedor());
        copia.setOrdenCompra(base.getOrdenCompra());
        copia.setMotivoMovimiento(base.getMotivoMovimiento());
        copia.setOrdenProduccion(base.getOrdenProduccion());
        copia.setOrdenProduccionEtapa(base.getOrdenProduccionEtapa());
        copia.setTipoMovimientoDetalle(base.getTipoMovimientoDetalle());
        copia.setOrdenCompraDetalle(base.getOrdenCompraDetalle());
        copia.setSolicitudMovimiento(base.getSolicitudMovimiento());
        copia.setRecepcionOc(base.getRecepcionOc());
        copia.setCodigoRecepcion(base.getCodigoRecepcion());
        copia.setCostoUnitarioAplicado(base.getCostoUnitarioAplicado());
        copia.setCostoTotalAplicado(base.getCostoTotalAplicado());
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
        boolean requiereAnalisis = requiereFisico(producto) || requiereQuimico(producto) || requiereMicro(producto);
        return requiereAnalisis ? EstadoLote.EN_CUARENTENA : EstadoLote.DISPONIBLE;
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

    private Long resolverAlmacenPreBodegaId() {
        if (preBodegaProduccionIdCacheLoaded) {
            return preBodegaProduccionIdCache;
        }
        preBodegaProduccionIdCacheLoaded = true;
        if (preBodegaId != null) {
            preBodegaProduccionIdCache = preBodegaId.longValue();
            return preBodegaProduccionIdCache;
        }
        Optional<Almacen> preBodega = almacenRepository.findByNombre("Pre-Bodega Producción");
        if (preBodega.isPresent() && preBodega.get().getId() != null) {
            preBodegaProduccionIdCache = preBodega.get().getId().longValue();
            return preBodegaProduccionIdCache;
        }
        preBodegaProduccionIdCache = PREBODEGA_PRODUCCION_ID_FALLBACK;
        log.warn("PREBODEGA_ID_NO_CONFIGURADA_USANDO_FALLBACK: {}", PREBODEGA_PRODUCCION_ID_FALLBACK);
        return preBodegaProduccionIdCache;
    }

    private boolean isTrasladoAPrebodega(TipoMovimiento tipoMovimiento,
                                         ClasificacionMovimientoInventario clasificacion,
                                         TipoMovimientoDetalle tipoMovimientoDetalle,
                                         Integer almacenDestinoId,
                                         Almacen almacenDestino,
                                         Long preBodegaProduccionId) {
        boolean destinoCoincide = (almacenDestinoId != null && preBodegaProduccionId != null
                && Objects.equals(almacenDestinoId.longValue(), preBodegaProduccionId));
        if (!destinoCoincide && almacenDestino != null) {
            destinoCoincide = PRE_BODEGA_PRODUCCION_NORMALIZADO.equals(normalizar(almacenDestino.getNombre()));
        }
        if (!destinoCoincide) {
            return false;
        }
        boolean esTipoDetalleTraslado = esTipoDetalleTrasladoPrebodega(tipoMovimientoDetalle);
        boolean esTipoDetalleTransferencia = esTipoDetalleTransferencia(tipoMovimientoDetalle);
        return tipoMovimiento == TipoMovimiento.TRANSFERENCIA
                || clasificacion == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION
                || esTipoDetalleTraslado
                || esTipoDetalleTransferencia;
    }

    private boolean esTipoDetalleTrasladoPrebodega(TipoMovimientoDetalle tipoMovimientoDetalle) {
        if (tipoMovimientoDetalle == null || tipoMovimientoDetalle.getDescripcion() == null) {
            return false;
        }
        String descripcionNormalizada = normalizar(tipoMovimientoDetalle.getDescripcion());
        return descripcionNormalizada.contains("prebodega")
                || descripcionNormalizada.contains("pre-bodega")
                || descripcionNormalizada.contains("pre bodega");
    }

    private boolean esTipoDetalleTransferencia(TipoMovimientoDetalle tipoMovimientoDetalle) {
        return tipoDetalleTransferenciaId != null
                && tipoMovimientoDetalle != null
                && tipoMovimientoDetalle.getId() != null
                && Objects.equals(tipoMovimientoDetalle.getId(), tipoDetalleTransferenciaId.longValue());
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
            validarDetalleCompleto(solicitud, det);
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
                throw new CustomBusinessException(
                        ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                        "SOLICITUD_DETALLE_INCOMPLETO",
                        Map.of(
                                "solicitudId", solicitud.getId(),
                                "detalleId", det.getId(),
                                "missingFields", List.of("lote_id")
                        )
                );
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
                throw loteStockInsuficienteException(loteOrigen, producto, pendiente, movible, loteOrigen.getAlmacen());
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
            if (!StringUtils.hasText(codigoLote)) {
                throw new CustomBusinessException(
                        ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                        "SOLICITUD_DETALLE_INCOMPLETO",
                        Map.of(
                                "solicitudId", solicitud.getId(),
                                "detalleId", det.getId(),
                                "missingFields", List.of("codigo_lote")
                        )
                );
            }
            LoteProducto loteDestino = ensureDestinoLote(
                    producto,
                    codigoLote,
                    loteOrigen,
                    almacenDestino,
                    null
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

        EstadoSolicitudMovimiento estadoFinal = todasAtendidas
                ? (solicitud.getOrdenProduccion() != null
                ? EstadoSolicitudMovimiento.CERRADA
                : EstadoSolicitudMovimiento.ATENDIDA)
                : EstadoSolicitudMovimiento.PARCIAL;
        solicitud.setEstado(estadoFinal);
        solicitud.setFechaResolucion(todasAtendidas ? LocalDateTime.now() : null);

        solicitudMovimientoRepository.saveAndFlush(solicitud);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteConsumoDTO> simulateFefo(Long productoId, BigDecimal cantidad, Long almacenId) {
        if (productoId == null || productoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCTO_ID_REQUERIDO");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO"));

        int escalaPermitida = catalogResolver.decimals(producto.getUnidadMedida());
        int escalaCantidad = Math.max(0, cantidad.stripTrailingZeros().scale());
        if (escalaCantidad > escalaPermitida) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CANTIDAD_NO_COMPATIBLE_UDM");
        }
        BigDecimal normalizada = cantidad.setScale(Math.max(cantidad.scale(), escalaPermitida), RoundingMode.HALF_UP);

        log.info("FEFO_PREVIEW productoId={} cantidad={} almacenId={}", productoId, normalizada, almacenId);

        List<FefoSelection> selecciones = calcularSeleccionesFefo(
                producto.getId().longValue(),
                normalizada,
                almacenId,
                Collections.emptySet());

        List<LoteConsumoDTO> consumos = selecciones.stream()
                .map(FefoSelection::toDto)
                .toList();

        if (!consumos.isEmpty()) {
            BigDecimal total = consumos.stream()
                    .map(LoteConsumoDTO::getTomar)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.debug("FEFO_PREVIEW_LOTES productoId={} lotes={} total={}",
                    productoId,
                    consumos.stream().map(LoteConsumoDTO::getLoteId).toList(),
                    total);
        } else {
            log.debug("FEFO_PREVIEW_LOTES productoId={} sin lotes elegibles", productoId);
        }

        return consumos;
    }

    private List<FefoSelection> calcularSeleccionesFefo(Long productoId,
                                                         BigDecimal cantidad,
                                                         Long almacenId,
                                                         Collection<Long> lotesExcluidos) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal restante = cantidad;
        List<LoteProducto> candidatos = cargarLotesFefo(productoId, almacenId);
        if (candidatos.isEmpty()) {
            if (hayLotesNoElegiblesConStock(productoId, almacenId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "LOTES_NO_ELEGIBLES");
            }
            return List.of();
        }

        List<FefoSelection> resultado = new ArrayList<>();
        Collection<Long> excluidos = lotesExcluidos != null ? lotesExcluidos : Collections.emptySet();

        for (LoteProducto lote : candidatos) {
            if (excluidos.contains(lote.getId())) {
                continue;
            }
            BigDecimal disponible = calcularDisponibleLote(lote);
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal tomar = disponible.min(restante);
            if (tomar.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal despues = disponible.subtract(tomar);
            if (despues.compareTo(BigDecimal.ZERO) < 0) {
                despues = BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
            }
            resultado.add(new FefoSelection(lote, disponible, tomar, despues));
            restante = restante.subtract(tomar);
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        if (resultado.isEmpty() && hayLotesNoElegiblesConStock(productoId, almacenId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LOTES_NO_ELEGIBLES");
        }

        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal disponibleTotal = cantidad.subtract(restante);
            log.warn("FEFO_STOCK_INSUFICIENTE productoId={} almacenId={} requerido={} disponible={}",
                    productoId, almacenId, cantidad, disponibleTotal);
            List<LoteConsumoDTO> detalle = resultado.stream()
                    .map(FefoSelection::toDto)
                    .toList();
            Map<String, Object> details = new HashMap<>();
            details.put("faltante", restante);
            details.put("lotes", detalle);
            throw new CustomBusinessException(ApiErrorCode.STOCK_INSUFICIENTE,
                    "Stock insuficiente para consumo FEFO",
                    details);
        }

        return resultado;
    }

    private List<LoteProducto> cargarLotesFefo(Long productoId, Long almacenId) {
        List<LoteProducto> lotes;
        if (almacenId != null) {
            lotes = loteProductoRepository
                    .findByProductoIdAndAlmacenIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
                            productoId,
                            Math.toIntExact(almacenId),
                            ESTADOS_FEFO_ELEGIBLES);
        } else {
            lotes = loteProductoRepository
                    .findByProductoIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
                            productoId,
                            ESTADOS_FEFO_ELEGIBLES);
        }
        if (lotes == null || lotes.isEmpty()) {
            return List.of();
        }
        Comparator<LoteProducto> comparator = Comparator
                .comparing((LoteProducto l) -> Optional.ofNullable(l.getFechaVencimiento())
                        .orElse(LocalDateTime.MAX))
                .thenComparing(l -> Optional.ofNullable(l.getFechaLiberacion())
                        .orElse(LocalDateTime.MIN))
                .thenComparing(LoteProducto::getId);
        lotes.sort(comparator);
        return lotes;
    }

    private boolean hayLotesNoElegiblesConStock(Long productoId, Long almacenId) {
        List<LoteProducto> noElegibles;
        if (almacenId != null) {
            noElegibles = loteProductoRepository.findByProductoIdAndAlmacenIdAndEstadoIn(
                    productoId,
                    Math.toIntExact(almacenId),
                    ESTADOS_FEFO_NO_ELEGIBLES);
        } else {
            noElegibles = loteProductoRepository.findByProductoIdAndEstadoIn(
                    productoId,
                    ESTADOS_FEFO_NO_ELEGIBLES);
        }
        if (noElegibles == null || noElegibles.isEmpty()) {
            return false;
        }
        return noElegibles.stream()
                .map(this::calcularDisponibleLote)
                .anyMatch(disponible -> disponible.compareTo(BigDecimal.ZERO) > 0);
    }

    private BigDecimal calcularReservaPendienteParaDetalle(SolicitudMovimiento solicitud,
                                                           SolicitudMovimientoDetalle detalleSolicitud,
                                                           LoteProducto loteOrigen) {
        if (solicitud == null || detalleSolicitud == null || loteOrigen == null) {
            return BigDecimal.ZERO;
        }

        if (detalleSolicitud.getLote() == null
                || !Objects.equals(detalleSolicitud.getLote().getId(), loteOrigen.getId())) {
            return BigDecimal.ZERO;
        }

        BigDecimal cantidad = Optional.ofNullable(detalleSolicitud.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal atendida = Optional.ofNullable(detalleSolicitud.getCantidadAtendida()).orElse(BigDecimal.ZERO);

        BigDecimal pendiente = cantidad.subtract(atendida);
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal reservadoActual = Optional.ofNullable(loteOrigen.getStockReservado()).orElse(BigDecimal.ZERO);

        BigDecimal reservaPendiente = pendiente.min(reservadoActual);
        if (reservaPendiente.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return reservaPendiente;
    }

    private BigDecimal calcularDisponibleLote(LoteProducto lote) {
        BigDecimal stock = Optional.ofNullable(lote.getStockLote()).orElse(BigDecimal.ZERO);
        BigDecimal reservado = Optional.ofNullable(lote.getStockReservado()).orElse(BigDecimal.ZERO);
        BigDecimal disponible = stock.subtract(reservado);
        if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        return disponible.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private Long obtenerAlmacenActualLoteId(LoteProducto lote) {
        if (lote == null || lote.getAlmacen() == null || lote.getAlmacen().getId() == null) {
            return null;
        }
        return lote.getAlmacen().getId().longValue();
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal obtenerCantidadReservadaPorEsteDetalle(SolicitudMovimientoDetalle detalle) {
        if (detalle == null) {
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        BigDecimal solicitada = nvl(detalle.getCantidad()).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal atendida = nvl(detalle.getCantidadAtendida()).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal pendiente = solicitada.subtract(atendida);
        BigDecimal referencia = pendiente.compareTo(BigDecimal.ZERO) > 0 ? pendiente : solicitada;
        if (referencia.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        return referencia.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private BigDecimal obtenerCantidadSolicitadaDelDetalle(SolicitudMovimientoDetalle detalle, BigDecimal respaldoDto) {
        if (detalle == null) {
            return nvl(respaldoDto).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        BigDecimal solicitada = nvl(detalle.getCantidad()).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal atendida = nvl(detalle.getCantidadAtendida()).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        BigDecimal pendiente = solicitada.subtract(atendida);
        if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
            return pendiente.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        if (solicitada.compareTo(BigDecimal.ZERO) > 0) {
            return solicitada.setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
        }
        return nvl(respaldoDto).setScale(CANTIDAD_SCALE, CANTIDAD_ROUNDING);
    }

    private SolicitudMovimientoDetalle validarDetalleCompatibleConLote(SolicitudMovimientoDetalle detalle,
                                                                      LoteProducto loteOrigen) {
        if (detalle == null || loteOrigen == null || loteOrigen.getId() == null) {
            return null;
        }
        if (detalle.getLote() == null || detalle.getLote().getId() == null) {
            return null;
        }
        if (!Objects.equals(detalle.getLote().getId(), loteOrigen.getId())) {
            return null;
        }
        return detalle;
    }

    // =====================================
    // CONSUMO AUTOMÁTICO DE INSUMOS POR OP
    // =====================================
    @Override
    @Transactional
    public void consumirInsumosPorOrden(Long ordenProduccionId, Long ordenProduccionEtapaId, Long usuarioId) {

        final Long preBodegaId = Objects.requireNonNull(
                catalogResolver.getAlmacenPreBodegaProduccionId(),
                "CONFIG_FALTANTE: inventory.almacenPreBodegaProduccionId");
        Long etapaDestinoId = resolverEtapaConsumo(ordenProduccionId, ordenProduccionEtapaId);

        // 1) Traer TODAS las solicitudes de la OP (cualquier estado) con sus partidas + lote
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

        // 2) Por cada partida, emitir SALIDA_PRODUCCION desde el lote ya trasladado a Pre-Bodega
        for (SolicitudMovimiento sol : solicitudes) {

            final Producto producto = sol.getProducto();
            if (producto == null || producto.getId() == null) {
                log.warn("CONSUMO_OP: solicitud sin producto. solId={}", sol.getId());
                continue;
            }
            ModoControlInventario modoControl = Optional.ofNullable(producto.getModoControlInventario())
                    .orElse(ModoControlInventario.CONTROL_STOCK);
            if (modoControl == ModoControlInventario.SIN_CONTROL_STOCK) {
                log.debug("CONSUMO_OP: producto sin control de stock, se omite consumo automático. solId={}, productoId={}",
                        sol.getId(), producto.getId());
                continue;
            }

            final Integer productoIdInt = producto.getId();
            final Long productoIdLong = productoIdInt.longValue();

            // Motivo / tipo-detalle: preferimos lo que traía la solicitud; si falta, usamos configuración general
            final Long motivoPreferido = (sol.getMotivoMovimiento() != null) ? sol.getMotivoMovimiento().getId() : null;
            final Long tipoDetPreferido = (sol.getTipoMovimientoDetalle() != null) ? sol.getTipoMovimientoDetalle().getId() : null;
            final Long motivoSalida = (motivoPreferido != null)
                    ? motivoPreferido
                    : catalogResolver.getMotivoSalidaProduccionId(); // puede ser null si no está configurado
            final Long tipoDetSalida = (tipoDetPreferido != null)
                    ? tipoDetPreferido
                    : Objects.requireNonNull(catalogResolver.getTipoDetalleSalidaId(),
                    "CONFIG_FALTANTE: inventory.tipoDetalle.salidaId");

            for (SolicitudMovimientoDetalle det : Optional.ofNullable(sol.getDetalles()).orElse(List.of())) {
                validarDetalleCompleto(sol.getId(), det);

                // Cantidad “a consumir” = atendida (si la hubo) o solicitada
                final BigDecimal qtySolicitada = Optional.ofNullable(det.getCantidad()).orElse(BigDecimal.ZERO);
                final BigDecimal qtyAtendida  = Optional.ofNullable(det.getCantidadAtendida()).orElse(BigDecimal.ZERO);
                final BigDecimal qtyObjetivo  = (qtyAtendida.signum() > 0) ? qtyAtendida : qtySolicitada;
                if (qtyObjetivo.signum() <= 0) {
                    continue;
                }
                if (det.getEstado() == EstadoSolicitudMovimientoDetalle.ATENDIDO
                        && qtyAtendida.compareTo(qtyObjetivo) >= 0) {
                    continue;
                }

                // Ubicar el lote equivalente en Pre-Bodega (mismo código de lote)
                final String codigoLote =
                        (det.getLote() != null && det.getLote().getCodigoLote() != null)
                                ? det.getLote().getCodigoLote()
                                : null;

                if (!StringUtils.hasText(codigoLote)) {
                    throw new CustomBusinessException(
                            ApiErrorCode.SOLICITUD_DETALLE_INCOMPLETO,
                            "SOLICITUD_DETALLE_INCOMPLETO",
                            Map.of(
                                    "solicitudId", sol.getId(),
                                    "detalleId", det.getId(),
                                    "missingFields", List.of("codigo_lote")
                            )
                    );
                }

                final Optional<LoteProducto> lotePreBodegaOpt = loteProductoRepository
                        .findByCodigoLoteAndProductoIdAndAlmacenId(
                                codigoLote, productoIdInt, preBodegaId.intValue());

                if (lotePreBodegaOpt.isEmpty()) {
                    log.warn("CONSUMO_OP: no existe lote en Pre-Bodega para consumir. op={}, prod={}, lote={}",
                            ordenProduccionId, productoIdInt, codigoLote);
                    throw new CustomBusinessException(
                            ApiErrorCode.CONSUMO_PREBODEGA_INSUFICIENTE,
                            "No hay lote disponible en Pre-Bodega para consumo automático",
                            Map.of(
                                    "ordenProduccionId", ordenProduccionId,
                                    "productoId", productoIdLong,
                                    "codigoLote", codigoLote
                            ));
                }
                final LoteProducto lotePreBodega = lotePreBodegaOpt.get();

                loteCalidadValidator.validarLoteUtilizable(lotePreBodega);

                // Idempotencia: resta SALIDAS ya emitidas para esta solicitud/producto/lote y tipo-detalle
                final BigDecimal yaConsumido = Optional.ofNullable(
                        repository.sumaPorSolicitudYTipo(
                                sol.getId(),
                                productoIdLong,
                                lotePreBodega.getId(),
                                TipoMovimiento.SALIDA,
                                tipoDetSalida,
                                null) // motivo no filtra idempotencia
                ).orElse(BigDecimal.ZERO);

                final BigDecimal pendiente = qtyObjetivo.subtract(yaConsumido);
                if (pendiente.signum() <= 0) {
                    log.debug("CONSUMO_OP IDEMP: sin pendiente solId={} loteId={} consumido={} objetivo={}",
                            sol.getId(), lotePreBodega.getId(), yaConsumido, qtyObjetivo);
                    continue;
                }

                BigDecimal disponible = calcularDisponibleLote(lotePreBodega);
                if (disponible.compareTo(pendiente) < 0) {
                    throw new CustomBusinessException(
                            ApiErrorCode.CONSUMO_PREBODEGA_INSUFICIENTE,
                            "No hay lote disponible en Pre-Bodega para consumo automático",
                            Map.of(
                                    "ordenProduccionId", ordenProduccionId,
                                    "productoId", productoIdLong,
                                    "codigoLote", codigoLote,
                                    "faltante", pendiente.subtract(disponible)
                            ));
                }

                // Registrar la SALIDA_PRODUCCION (desde Pre-Bodega). Dejamos ligada la solicitud para idempotencia.
                final MovimientoInventarioDTO dtoSalida = new MovimientoInventarioDTO(
                        null,                               // id
                        pendiente,                          // cantidad
                        TipoMovimiento.SALIDA,              // tipo
                        ClasificacionMovimientoInventario.SALIDA_PRODUCCION, // clasif
                        null,                               // docReferencia
                        null,                               // destinoTexto
                        null,                               // clienteNombre
                        null,                               // causaDevolucionPt
                        null,                               // condicionProductoDevuelto
                        productoIdInt,                      // productoId (Integer)
                        lotePreBodega.getId(),              // loteProductoId (Long)
                        preBodegaId.intValue(),             // almacenOrigenId (Pre-Bodega)
                        null,                               // almacenDestinoId
                        null,                               // proveedorId
                        null,                               // ordenCompraId
                        motivoSalida,                       // motivoMovimientoId (puede ser null)
                        tipoDetSalida,                      // tipoMovimientoDetalleId (obligatorio)
                        sol.getId(),                        // solicitudMovimientoId  <-- clave para idempotencia
                        usuarioId,                          // usuarioId
                        ordenProduccionId,                  // ordenProduccionId
                        etapaDestinoId,                     // ordenProduccionEtapaId
                        null,                               // ordenCompraDetalleId
                        null,                               // codigoLote
                        null,                               // fechaVencimiento
                        null,                               // estadoLote
                        null,                               // autoSplit
                        null,                               // atenciones
                        null,                               // loteLegacy
                        null                                // ubicacionDestinoId
                );

                // Usa la rama especial añadida en registrarMovimiento para SALIDA_PRODUCCION
                this.registrarMovimiento(dtoSalida);

                // Marcar el detalle: si ya quedó cubierto, ATENDIDO (no rompe en reentradas)
                det.setEstado(com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle.ATENDIDO);
            }
        }
    }

    private String resolverNombreEtapaProduccion(Long etapaId) {
        if (etapaId == null) {
            return null;
        }
        return etapaProduccionRepository.findById(etapaId)
                .map(EtapaProduccion::getNombre)
                .orElse(null);
    }

    private Long resolverEtapaConsumo(Long ordenProduccionId, Long etapaId) {
        EtapaProduccion etapaActiva = resolverEtapaActiva(ordenProduccionId, etapaId);
        return etapaActiva.getId();
    }

    private boolean requiereEtapaActiva(ClasificacionMovimientoInventario clasificacion,
                                        Long tipoMovimientoDetalleId,
                                        Long etapaId,
                                        Long ordenProduccionId) {
        if (ordenProduccionId == null) {
            return false;
        }
        if (etapaId != null) {
            return true;
        }
        if (clasificacion != null) {
            return clasificacion == ClasificacionMovimientoInventario.SALIDA_PRODUCCION;
        }
        Long tipoDetalleSalidaProduccionId = catalogResolver.getTipoDetalleSalidaProduccionId();
        return tipoDetalleSalidaProduccionId != null
                && tipoMovimientoDetalleId != null
                && Objects.equals(tipoMovimientoDetalleId, tipoDetalleSalidaProduccionId);
    }

    private EtapaProduccion resolverEtapaActiva(Long ordenProduccionId, Long etapaId) {
        if (ordenProduccionId == null) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                    "OP_SIN_ETAPA_ACTIVA",
                    Map.of("ordenProduccionId", (Object) null)
            );
        }
        if (etapaId != null) {
            EtapaProduccion etapa = etapaProduccionRepository.findById(etapaId)
                    .orElseThrow(() -> new CustomBusinessException(
                            ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                            "OP_SIN_ETAPA_ACTIVA",
                            Map.of("ordenProduccionId", ordenProduccionId, "etapaId", etapaId)
                    ));
            boolean finalizada = etapa.getFechaInicio() != null
                    && (etapa.getFechaFin() != null || etapa.getEstado() == EstadoEtapa.FINALIZADA);
            if (finalizada && etapa.getOrdenProduccion() != null
                    && Objects.equals(etapa.getOrdenProduccion().getId(), ordenProduccionId)) {
                return etapa;
            }
            return validarEtapaActiva(etapa, ordenProduccionId, false);
        }
        long activas = etapaProduccionRepository.countByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(ordenProduccionId);
        List<EtapaProduccion> candidatas = activas > 0
                ? etapaProduccionRepository.findByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNull(ordenProduccionId)
                : List.of();
        if (activas > 1) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_MULTIPLES_ETAPAS_ACTIVAS,
                    "OP_MULTIPLES_ETAPAS_ACTIVAS",
                    Map.of("ordenProduccionId", ordenProduccionId,
                            "etapas", candidatas.stream().map(EtapaProduccion::getId).toList())
            );
        }
        Optional<EtapaProduccion> etapaActiva = etapaProduccionRepository
                .findTopByOrdenProduccionIdAndFechaInicioIsNotNullAndFechaFinIsNullOrderByFechaInicioDescIdDesc(ordenProduccionId);
        if (etapaActiva.isPresent()) {
            return validarEtapaActiva(etapaActiva.get(), ordenProduccionId, true);
        }
        throw new CustomBusinessException(
                ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                "OP_SIN_ETAPA_ACTIVA",
                Map.of("ordenProduccionId", ordenProduccionId)
        );
    }

    private EtapaProduccion validarEtapaActiva(EtapaProduccion etapa, Long ordenProduccionId, boolean permitirHeuristicaFecha) {
        if (etapa == null) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                    "OP_SIN_ETAPA_ACTIVA",
                    Map.of("ordenProduccionId", ordenProduccionId,
                            "etapaId", (Object) null)
            );
        }
        if (etapa.getOrdenProduccion() != null
                && etapa.getOrdenProduccion().getId() != null
                && !Objects.equals(etapa.getOrdenProduccion().getId(), ordenProduccionId)) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                    "OP_SIN_ETAPA_ACTIVA",
                    Map.of("ordenProduccionId", ordenProduccionId,
                            "etapaId", etapa.getId())
            );
        }
        boolean activaPorEstado = etapa.getEstado() == com.willyes.clemenintegra.produccion.model.enums.EstadoEtapa.EN_PROCESO;
        boolean activaPorFecha = etapa.getFechaInicio() != null && etapa.getFechaFin() == null;
        if (!activaPorEstado && !(permitirHeuristicaFecha && activaPorFecha)) {
            throw new CustomBusinessException(
                    ApiErrorCode.OP_SIN_ETAPA_ACTIVA,
                    "OP_SIN_ETAPA_ACTIVA",
                    Map.of("ordenProduccionId", ordenProduccionId,
                            "etapaId", etapa.getId())
            );
        }
        return etapa;
    }


}
