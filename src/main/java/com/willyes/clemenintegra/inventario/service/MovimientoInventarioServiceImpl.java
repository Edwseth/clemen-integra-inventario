package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioFiltroDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.MovimientoInventarioMapper;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.inventario.model.enums.EstadoOrdenCompra;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
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
import java.util.EnumSet;
import java.util.List;
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

    @Resource
    private final EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Intento de registrar movimiento sin autenticación válida");
            throw new AuthenticationCredentialsNotFoundException("No se encontró autenticación válida");
        }

        if (dto.tipoMovimientoDetalleId() == null) {
            throw new IllegalArgumentException("tipo_movimiento_detalle_id es obligatorio"); // [Codex Edit]
        }

        if (dto.tipoMovimiento() == TipoMovimiento.ENTRADA
                && Objects.equals(dto.motivoMovimientoId(), catalogResolver.getMotivoIdEntradaProductoTerminado())
                && dto.ordenProduccionId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "ENTRADA_PT_REQUIERE_ORDEN_PRODUCCION_ID");
        }

        MovimientoInventario movimiento = mapper.toEntity(dto);

        TipoMovimientoDetalle tipoMovimientoDetalle = null;
        if (dto.tipoMovimientoDetalleId() != null) {
            tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(dto.tipoMovimientoDetalleId())
                    .orElseThrow(() -> new NoSuchElementException("Tipo de detalle de movimiento no encontrado"));
            if (requiereSolicitudMovimientoId(tipoMovimientoDetalle) && dto.solicitudMovimientoId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "SOLICITUD_MOVIMIENTO_ID_REQUERIDO");
            }
        }

        // 1. Cargar entidades principales
        Producto producto = productoRepository.findById(dto.productoId().longValue())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        Almacen almacenOrigen = dto.almacenOrigenId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenOrigenId()) : null;

        Almacen almacenDestino = dto.almacenDestinoId() != null
                ? entityManager.getReference(Almacen.class, dto.almacenDestinoId()) : null;

        Usuario usuario = usuarioService.obtenerUsuarioAutenticado();

        OrdenProduccion ordenProduccion = dto.ordenProduccionId() != null
                ? entityManager.getReference(OrdenProduccion.class, dto.ordenProduccionId())
                : null;

        TipoMovimiento tipoMovimiento = dto.tipoMovimiento();
        ClasificacionMovimientoInventario clasificacion = dto.clasificacionMovimientoInventario();

        log.debug("MOV-REQ tipo={}, clasificacion={}, prod={}, qty={}, opId={}",
                tipoMovimiento, clasificacion, dto.productoId(), dto.cantidad(), dto.ordenProduccionId());

        if (clasificacion == ClasificacionMovimientoInventario.SALIDA_PRODUCCION
                && tipoMovimiento != TipoMovimiento.SALIDA) {
            log.warn("INCONSISTENT_MOVEMENT: tipo={}, clasificacion={}, opId={}",
                    tipoMovimiento, clasificacion, dto.ordenProduccionId());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "INCONSISTENT_MOVEMENT: SALIDA_PRODUCCION requiere tipoMovimiento=SALIDA");
        }

        SolicitudMovimiento solicitud = null;
        if (dto.solicitudMovimientoId() != null) {
            solicitud = solicitudMovimientoRepository.findById(dto.solicitudMovimientoId())
                    .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));

            Long solicitudProductoId = solicitud.getProducto() != null ? Long.valueOf(solicitud.getProducto().getId()) : null;
            if (!Objects.equals(solicitudProductoId, dto.productoId() != null ? dto.productoId().longValue() : null)) {
                log.warn("MISMATCH_PRODUCTO_ID: esperado={}, recibido={}", solicitudProductoId, dto.productoId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_PRODUCTO_ID");
            }

            if (solicitud.getTipoMovimiento() != dto.tipoMovimiento()) {
                log.warn("MISMATCH_TIPO_MOVIMIENTO: esperado={}, recibido={}", solicitud.getTipoMovimiento(), dto.tipoMovimiento());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_TIPO_MOVIMIENTO");
            }

            Long solicitudAlmacenOrigenId = solicitud.getAlmacenOrigen() != null ? Long.valueOf(solicitud.getAlmacenOrigen().getId()) : null;
            Long dtoAlmacenOrigenId = dto.almacenOrigenId() != null ? dto.almacenOrigenId().longValue() : null;
            if (solicitudAlmacenOrigenId != null && !Objects.equals(solicitudAlmacenOrigenId, dtoAlmacenOrigenId)) {
                log.warn("MISMATCH_ALMACEN_ORIGEN_ID: esperado={}, recibido={}", solicitudAlmacenOrigenId, dto.almacenOrigenId());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MISMATCH_ALMACEN_ORIGEN_ID");
            }

            Long solicitudAlmacenDestinoId = solicitud.getAlmacenDestino() != null ? Long.valueOf(solicitud.getAlmacenDestino().getId()) : null;
            Long dtoAlmacenDestinoId = dto.almacenDestinoId() != null ? dto.almacenDestinoId().longValue() : null;
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
                } else if (solicitud.getEstado() != EstadoSolicitudMovimiento.AUTORIZADA) {
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

        if (tipoMovimiento == TipoMovimiento.RECEPCION) {
            if (dto.ordenCompraId() == null) {
                throw new IllegalArgumentException("Las recepciones sin Orden de Compra deben usar un lote existente");
            }
            LoteProducto loteRecepcion = crearLoteRecepcion(dto, producto, almacenDestino, usuario,
                    cantidadSolicitada, motivoMovimiento);
            lotesProcesados = List.of(new MovimientoLoteDetalle(loteRecepcion, cantidadSolicitada));
        } else {
            lotesProcesados = procesarMovimientoConLoteExistente(dto, tipoMovimiento, almacenOrigen,
                    almacenDestino, producto, cantidadSolicitada, devolucionInterna, solicitud);
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
        movimiento.setTipoMovimientoDetalle(tipoMovimientoDetalle != null
                ? tipoMovimientoDetalle
                : dto.tipoMovimientoDetalleId() != null
                ? entityManager.getReference(TipoMovimientoDetalle.class, dto.tipoMovimientoDetalleId())
                : null);
        movimiento.setRegistradoPor(usuario);
        if (solicitud != null) {
            movimiento.setSolicitudMovimiento(solicitud);
        }

        MovimientoInventario guardado = repository.save(movimiento);

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

        if (solicitud != null) {
            if (solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA) {
                solicitud.setEstado(EstadoSolicitudMovimiento.ATENDIDA);
            } else {
                solicitud.setEstado(EstadoSolicitudMovimiento.EJECUTADA);
            }
            List<SolicitudMovimientoDetalle> detallesSolicitud = solicitud.getDetalles();
            if (detallesSolicitud != null && !detallesSolicitud.isEmpty()) {
                List<SolicitudMovimientoDetalle> detallesActualizados = new ArrayList<>();
                for (SolicitudMovimientoDetalle detalle : detallesSolicitud) {
                    if (detalle == null) {
                        continue;
                    }
                    BigDecimal nuevaCantidadAtendida = detalle.getCantidad() != null
                            ? detalle.getCantidad()
                            : guardado.getCantidad();
                    detalle.setCantidadAtendida(nuevaCantidadAtendida);
                    log.debug("DETALLE_SOLICITUD_ACTUALIZADO id={} cantidadSolicitada={} cantidadAtendida={}",
                            detalle.getId(), detalle.getCantidad(), nuevaCantidadAtendida);
                    detallesActualizados.add(detalle);
                }
                if (!detallesActualizados.isEmpty()) {
                    solicitudMovimientoDetalleRepository.saveAll(detallesActualizados);
                }
            }
            solicitudMovimientoRepository.save(solicitud);
        }
        return mapper.safeToResponseDTO(guardado);
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

        // NUEVO: estilo con 2 decimales
        DataFormat df = workbook.createDataFormat();
        CellStyle styleDec2 = workbook.createCellStyle();
        styleDec2.setDataFormat(df.getFormat("0.00"));

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
                cCant.setCellValue(cant.setScale(2, RoundingMode.HALF_UP).doubleValue());
                cCant.setCellStyle(styleDec2);
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
            // NUEVO: estilo con 2 decimales
            DataFormat df = workbook.createDataFormat();
            CellStyle styleDec2 = workbook.createCellStyle();
            styleDec2.setDataFormat(df.getFormat("0.00"));
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
                    c2.setCellValue(cant.setScale(2, RoundingMode.HALF_UP).doubleValue());
                    c2.setCellStyle(styleDec2);
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

        LoteProducto loteOrigen = loteProductoRepository.findById(dto.loteProductoId())
                .orElseThrow(() -> {
                    log.warn(
                            "procesarMovimientoConLoteExistente: lote no encontrado loteId={} productoId={}",
                            dto.loteProductoId(), producto.getId());
                    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_NO_ENCONTRADO");
                });

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

        boolean esTransferenciaInternaProduccion = tipo == TipoMovimiento.TRANSFERENCIA
                && dto.clasificacionMovimientoInventario() == ClasificacionMovimientoInventario.TRANSFERENCIA_INTERNA_PRODUCCION;
        boolean autoSplitSolicitado = Boolean.TRUE.equals(dto.autoSplit());

        if (EnumSet.of(TipoMovimiento.SALIDA, TipoMovimiento.TRANSFERENCIA,
                TipoMovimiento.DEVOLUCION, TipoMovimiento.AJUSTE).contains(tipo)) {
            if (solicitud != null && solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA) {
                if (reservadoActual.compareTo(cantidad) < 0) {
                    log.warn("RESERVA_INSUFICIENTE: loteId={} reservado={} solicitado={} productoId={}",
                            loteOrigen.getId(), reservadoActual, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "RESERVA_INSUFICIENTE");
                }
            } else if (!(tipo == TipoMovimiento.TRANSFERENCIA
                    && esTransferenciaInternaProduccion
                    && autoSplitSolicitado
                    && cantidad.compareTo(disponibleNoNegativo) > 0)) {
                if (disponibleNoNegativo.compareTo(cantidad) < 0) {
                    log.warn("Stock insuficiente en lote: loteId={} disponible={} solicitado={} productoId={}",
                            loteOrigen.getId(), disponibleNoNegativo, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_STOCK_INSUFICIENTE");
                }
            }
        }

        if (tipo == TipoMovimiento.SALIDA) {
            log.debug("MOV-SALIDA descontando prod={}, qty={}, opId={}",
                    dto.productoId(), cantidad, dto.ordenProduccionId());
            loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidad));
            if (solicitud != null && solicitud.getEstado() == EstadoSolicitudMovimiento.RESERVADA) {
                loteOrigen.setStockReservado(reservadoActual.subtract(cantidad));
            }
            if (loteOrigen.getStockLote().compareTo(BigDecimal.ZERO) <= 0) {
                loteOrigen.setAgotado(true);
                if (loteOrigen.getFechaAgotado() == null) {
                    loteOrigen.setFechaAgotado(LocalDateTime.now());
                }
            }
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

            if (cantidad.compareTo(disponibleNoNegativo) <= 0 || !esTransferenciaInternaProduccion || !autoSplitSolicitado) {
                if (cantidad.compareTo(disponibleNoNegativo) > 0) {
                    log.warn("Stock insuficiente en lote: loteId={} disponible={} solicitado={} productoId={}",
                            loteOrigen.getId(), disponibleNoNegativo, cantidad, producto.getId());
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "LOTE_STOCK_INSUFICIENTE");
                }
                MovimientoLoteDetalle detalle = ejecutarTransferenciaDesdeLote(loteOrigen, destino, producto, cantidad);
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
                detalles.add(ejecutarTransferenciaDesdeLote(loteOrigen, destino, producto, cantidadInicial));
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
                    LoteProducto loteAdicional = loteProductoRepository.findById(par.loteId())
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

                    MovimientoLoteDetalle parcial = ejecutarTransferenciaDesdeLote(loteAdicional, destino, producto, par.cantidad());
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
                                                                 BigDecimal cantidadTransferir) {
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ALMACEN_DESTINO_REQUERIDO");
        }
        int comparacion = Optional.ofNullable(loteOrigen.getStockLote()).orElse(BigDecimal.ZERO)
                .compareTo(cantidadTransferir);
        if (comparacion > 0) {
            loteOrigen.setStockLote(loteOrigen.getStockLote().subtract(cantidadTransferir));
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

            BigDecimal nuevoStock = Optional.ofNullable(loteDestino.getStockLote()).orElse(BigDecimal.ZERO)
                    .add(cantidadTransferir);
            loteDestino.setStockLote(nuevoStock);

            if (destino.getCategoria() == TipoCategoria.OBSOLETOS) {
                loteDestino.setEstado(EstadoLote.RECHAZADO);
            }

            LoteProducto guardadoDestino = loteProductoRepository.save(loteDestino);
            return new MovimientoLoteDetalle(guardadoDestino, cantidadTransferir);
        }

        loteOrigen.setAlmacen(destino);
        if (destino.getCategoria() == TipoCategoria.OBSOLETOS) {
            loteOrigen.setEstado(EstadoLote.RECHAZADO);
        }
        LoteProducto guardado = loteProductoRepository.save(loteOrigen);
        return new MovimientoLoteDetalle(guardado, cantidadTransferir);
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
                .findByProductoIdAndAlmacenesIdAndEstadoInOrderByFechaVencimientoAscIdAsc(
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

}
