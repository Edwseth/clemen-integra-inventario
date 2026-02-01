package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimientoDetalle;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.repository.SolicitudMovimientoDetalleRepository.OpDetalleCount;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;

import com.willyes.clemenintegra.shared.exception.CustomBusinessException;

@Service
@RequiredArgsConstructor
public class SolicitudMovimientoServiceImpl implements SolicitudMovimientoService {

    private static final List<EstadoProduccion> ESTADOS_OP_CERRADOS = List.of(
            EstadoProduccion.FINALIZADA,
            EstadoProduccion.CANCELADA,
            EstadoProduccion.CERRADA_INCOMPLETA
    );

    private final SolicitudMovimientoRepository repository;
    private final SolicitudMovimientoDetalleRepository solicitudMovimientoDetalleRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;
    private final AlmacenRepository almacenRepository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ReservaLoteService reservaLoteService;
    private final LoteCalidadValidator loteCalidadValidator;

    @Value("${app.inventario.prebodega.id:6}")
    private Integer preBodegaProduccionId;

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO registrarSolicitud(SolicitudMovimientoRequestDTO dto) {
        validarAutenticacion();
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado"));

        LoteProducto lote;
        // CODEx: actualmente solo selecciona un primer lote disponible sin considerar FIFO global
        if (dto.getLoteId() != null) {
            lote = loteRepository.findById(dto.getLoteId())
                    .orElseThrow(() -> new NoSuchElementException("Lote no encontrado"));
        } else {
            lote = loteRepository
                    .findFirstByProductoIdAndEstadoAndStockLoteGreaterThanOrderByFechaVencimientoAsc(
                            producto.getId(),
                            EstadoLote.DISPONIBLE,
                            BigDecimal.ZERO
                    )
                    .orElseThrow(() -> new NoSuchElementException(
                            "No hay stock disponible para el producto: " + producto.getNombre()
                    ));
        }

        loteCalidadValidator.validarLoteUtilizable(lote);

        Almacen origen;
        if (dto.getAlmacenOrigenId() != null) {
            origen = almacenRepository.findById(dto.getAlmacenOrigenId())
                    .orElseThrow(() -> new NoSuchElementException("Almacén origen no encontrado"));
        } else {
            origen = Optional.of(lote.getAlmacen())
                    .orElseThrow(() -> new NoSuchElementException("Almacén origen no encontrado"));
        }
        Almacen destino = null;
        if (dto.getAlmacenDestinoId() != null) {
            destino = almacenRepository.findById(dto.getAlmacenDestinoId())
                    .orElseThrow(() -> new NoSuchElementException("Almacén destino no encontrado"));
        }
        OrdenProduccion orden = null;
        if (dto.getOrdenProduccionId() != null) {
            orden = ordenProduccionRepository.findById(dto.getOrdenProduccionId())
                    .orElseThrow(() -> new NoSuchElementException("Orden de producción no encontrada"));
        }
        Usuario solicitante = usuarioRepository.findById(dto.getUsuarioSolicitanteId())
                .orElseThrow(() -> new NoSuchElementException("Usuario solicitante no encontrado"));
        Usuario responsable = null;
        if (dto.getUsuarioResponsableId() != null) {
            responsable = usuarioRepository.findById(dto.getUsuarioResponsableId())
                    .orElseThrow(() -> new NoSuchElementException("Usuario responsable no encontrado"));
        }

        MotivoMovimiento motivoMovimiento = null;
        if (dto.getMotivoMovimientoId() != null) {
            motivoMovimiento = motivoMovimientoRepository.findById(dto.getMotivoMovimientoId())
                    .orElseThrow(() -> new NoSuchElementException("Motivo de movimiento no encontrado"));
        }
        TipoMovimientoDetalle tipoMovimientoDetalle = null;
        if (dto.getTipoMovimientoDetalleId() != null) {
            tipoMovimientoDetalle = tipoMovimientoDetalleRepository.findById(dto.getTipoMovimientoDetalleId())
                    .orElseThrow(() -> new NoSuchElementException("Tipo de detalle de movimiento no encontrado"));
        }

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .tipoMovimiento(dto.getTipoMovimiento())
                .producto(producto)
                .lote(lote)
                .cantidad(dto.getCantidad())
                .almacenOrigen(origen)
                .almacenDestino(destino)
                .motivoMovimiento(motivoMovimiento)
                .tipoMovimientoDetalle(tipoMovimientoDetalle)
                .ordenProduccion(orden)
                .usuarioSolicitante(solicitante)
                .usuarioResponsable(responsable)
                .observaciones(dto.getObservaciones())
                .build();

        final boolean esParaOP = solicitud.getOrdenProduccion() != null || dto.getOrdenProduccionId() != null;
        final String mensajePreBodegaInexistente = "No existe Pre-Bodega Producción";
        if (esParaOP && solicitud.getAlmacenDestino() == null) {
            if (preBodegaProduccionId == null) {
                throw new CustomBusinessException(mensajePreBodegaInexistente);
            }
            Long preBodegaId = preBodegaProduccionId.longValue();
            Almacen preBodega = almacenRepository.findById(preBodegaId)
                    .orElseThrow(() -> new CustomBusinessException(
                            mensajePreBodegaInexistente + " id=" + preBodegaProduccionId));
            solicitud.setAlmacenDestino(preBodega);
        }

        if (esParaOP && solicitud.getAlmacenDestino() == null) {
            throw new CustomBusinessException(mensajePreBodegaInexistente);
        }

        if (solicitud.getDetalles() != null && solicitud.getAlmacenDestino() != null) {
            for (SolicitudMovimientoDetalle detalle : solicitud.getDetalles()) {
                if (detalle.getAlmacenDestino() == null) {
                    detalle.setAlmacenDestino(solicitud.getAlmacenDestino());
                }
            }
        }

        SolicitudMovimiento guardada = repository.saveAndFlush(solicitud);
        reservaLoteService.sincronizarReservasSolicitud(guardada);
        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    // servicio que alimenta el listado de solicitudes con paginación y filtros
    public Page<SolicitudMovimientoListadoDTO> listarSolicitudes(EstadoSolicitudMovimiento estado,
                                                                 String busqueda,
                                                                 Long almacenOrigenId,
                                                                 Long almacenDestinoId,
                                                                 Long ordenProduccionId,
                                                                 LocalDateTime desde,
                                                                 LocalDateTime hasta,
                                                                 Pageable pageable) {
        Specification<SolicitudMovimiento> spec = null;

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }

        if (estado != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) -> cb.equal(root.get("estado"), estado);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        if (almacenOrigenId != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) ->
                    cb.equal(root.join("almacenOrigen", jakarta.persistence.criteria.JoinType.LEFT).get("id"), almacenOrigenId);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        if (almacenDestinoId != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) ->
                    cb.equal(root.join("almacenDestino", jakarta.persistence.criteria.JoinType.LEFT).get("id"), almacenDestinoId);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        if (ordenProduccionId != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) ->
                    cb.equal(root.join("ordenProduccion", jakarta.persistence.criteria.JoinType.LEFT).get("id"), ordenProduccionId);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        LocalDateTime inicio = desde;
        LocalDateTime fin = hasta;
        if (inicio != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), inicio);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        if (fin != null) {
            Specification<SolicitudMovimiento> filtro = (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fechaSolicitud"), fin);
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            String like = "%" + busqueda.toLowerCase() + "%";
            Specification<SolicitudMovimiento> filtro = (root, query, cb) -> {
                var productoJoin = root.join("producto", jakarta.persistence.criteria.JoinType.LEFT);
                var solicitanteJoin = root.join("usuarioSolicitante", jakarta.persistence.criteria.JoinType.LEFT);
                var ordenJoin = root.join("ordenProduccion", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(productoJoin.get("nombre")), like),
                        cb.like(cb.lower(solicitanteJoin.get("nombreCompleto")), like),
                        cb.like(cb.lower(ordenJoin.get("codigoOrden")), like)
                );
            };
            spec = spec == null ? Specification.where(filtro) : spec.and(filtro);
        }

        Page<SolicitudMovimiento> page = repository.findAll(
                spec == null ? Specification.where((root, query, cb) -> cb.conjunction()) : spec,
                pageable
        );

        List<SolicitudMovimiento> solicitudes = page.getContent();

        List<Long> opIds = solicitudes.stream()
                .map(SolicitudMovimiento::getOrdenProduccion)
                .filter(Objects::nonNull)
                .map(OrdenProduccion::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Integer> countsByOp = opIds.isEmpty()
                ? Collections.emptyMap()
                : solicitudMovimientoDetalleRepository.countByOpIds(opIds)
                .stream()
                .collect(Collectors.toMap(
                        OpDetalleCount::getOpId,
                        c -> {
                            Long value = c.getCnt();
                            return value == null ? 0 : Math.toIntExact(value);
                        }
                ));

        List<SolicitudMovimientoListadoDTO> dtos = solicitudes.stream()
                .map(solicitud -> {
                    SolicitudMovimientoListadoDTO dto = toListadoDTO(solicitud);
                    OrdenProduccion orden = solicitud.getOrdenProduccion();
                    Long opId = orden != null ? orden.getId() : null;
                    int cnt = opId != null ? countsByOp.getOrDefault(opId, 0) : 0;
                    dto.setItemsCount(cnt);
                    try {
                        dto.getClass().getMethod("setItems", Integer.class).invoke(dto, cnt);
                    } catch (Exception ignored) {
                    }
                    return dto;
                })
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudMovimientoResponseDTO obtenerSolicitud(Long id) {
        var solicitud = repository.findWithDetalles(id)
                .orElseGet(() -> repository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada: " + id)));

        SolicitudMovimientoResponseDTO dto = toResponse(solicitud);
        if (solicitud.getProducto() != null) {
            dto.setCodigoSku(solicitud.getProducto().getCodigoSku());
        }
        if (solicitud.getOrdenProduccion() != null) {
            dto.setCodigoOrden(solicitud.getOrdenProduccion().getCodigoOrden());
        }
        return dto;
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId) {
        validarAutenticacion();
        SolicitudMovimiento solicitud = repository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudMovimiento.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        if (solicitud.getMotivoMovimiento() == null) {
            throw new IllegalArgumentException("La solicitud no tiene un motivo de movimiento asignado.");
        }
        if (solicitud.getTipoMovimientoDetalle() == null) {
            throw new IllegalArgumentException("Falta tipo de detalle de movimiento");
        }
        Usuario responsable = usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        solicitud.setUsuarioResponsable(responsable);
        solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        SolicitudMovimiento actualizada = repository.saveAndFlush(solicitud);
        reservaLoteService.sincronizarReservasSolicitud(actualizada);
        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO rechazarSolicitud(Long id, Long responsableId, String observaciones) {
        validarAutenticacion();
        SolicitudMovimiento solicitud = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudMovimiento.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        Usuario responsable = usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        solicitud.setUsuarioResponsable(responsable);
        solicitud.setEstado(EstadoSolicitudMovimiento.RECHAZADO);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setObservaciones(observaciones);
        SolicitudMovimiento actualizada = repository.save(solicitud);
        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO revertirAutorizacion(Long id, Long responsableId) {
        validarAutenticacion();
        SolicitudMovimiento solicitud = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudMovimiento.AUTORIZADA) {
            throw new IllegalStateException("La solicitud no está autorizada");
        }
        if (movimientoInventarioRepository.existsBySolicitudMovimientoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya tiene un movimiento registrado");
        }
        solicitud.setEstado(EstadoSolicitudMovimiento.PENDIENTE);
        solicitud.setUsuarioResponsable(null);
        solicitud.setFechaResolucion(null);
        SolicitudMovimiento actualizada = repository.save(solicitud);
        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO autorizarSolicitudCompleta(Long solicitudId, Long usuarioAutorizadorId) {
        validarAutenticacion();
        SolicitudMovimiento solicitud = repository.findByIdWithLock(solicitudId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitudMovimiento.CANCELADA
                || solicitud.getEstado() == EstadoSolicitudMovimiento.CERRADA
                || solicitud.getEstado() == EstadoSolicitudMovimiento.RECHAZADO) {
            throw new IllegalStateException("La solicitud no admite nuevas autorizaciones en su estado actual: "
                    + solicitud.getEstado());
        }

        Usuario responsable = usuarioRepository.findById(usuarioAutorizadorId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        int totalPendientes = 0;
        int totalAutorizados = 0;
        List<String> errores = new ArrayList<>();

        List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles())
                .orElseGet(Collections::emptyList);
        for (SolicitudMovimientoDetalle detalle : detalles) {
            if (!estaPendienteDeAutorizacion(detalle)) {
                continue;
            }

            totalPendientes++;

            try {
                autorizarDetalleInterno(solicitud, detalle, responsable);
                totalAutorizados++;
            } catch (RuntimeException ex) {
                String nombreProducto = obtenerNombreProducto(detalle, solicitud);
                errores.add(nombreProducto + ": " + ex.getMessage());
            }
        }

        if (totalPendientes > 0) {
            solicitud.setUsuarioResponsable(responsable);
            solicitud.setFechaResolucion(LocalDateTime.now());

            if (totalAutorizados == totalPendientes && errores.isEmpty()) {
                solicitud.setEstado(EstadoSolicitudMovimiento.AUTORIZADA);
            } else if (totalAutorizados > 0 || !errores.isEmpty()) {
                solicitud.setEstado(EstadoSolicitudMovimiento.PARCIAL);
            }
        }

        SolicitudMovimiento actualizada = repository.saveAndFlush(solicitud);
        reservaLoteService.sincronizarReservasSolicitud(actualizada);
        return toResponse(actualizada);
    }

    private void autorizarDetalleInterno(SolicitudMovimiento solicitud,
                                         SolicitudMovimientoDetalle detalle,
                                         Usuario responsable) {
        if (solicitud.getEstado() == EstadoSolicitudMovimiento.CANCELADA
                || solicitud.getEstado() == EstadoSolicitudMovimiento.CERRADA
                || solicitud.getEstado() == EstadoSolicitudMovimiento.RECHAZADO) {
            throw new IllegalStateException("La solicitud no admite nuevas autorizaciones");
        }

        if (!estaPendienteDeAutorizacion(detalle)) {
            throw new IllegalStateException("El detalle ya fue procesado");
        }
        if (detalle.getLote() == null || detalle.getLote().getId() == null) {
            throw new IllegalArgumentException("El detalle no tiene lote asignado");
        }

        solicitud.setUsuarioResponsable(responsable);
        reservaLoteService.crearOActualizarDesdeDetalle(detalle);
    }

    private boolean estaPendienteDeAutorizacion(SolicitudMovimientoDetalle detalle) {
        EstadoSolicitudMovimientoDetalle estado = detalle != null ? detalle.getEstado() : null;
        return estado == null
                || estado == EstadoSolicitudMovimientoDetalle.PENDIENTE
                || estado == EstadoSolicitudMovimientoDetalle.PARCIAL;
    }

    private String obtenerNombreProducto(SolicitudMovimientoDetalle detalle, SolicitudMovimiento solicitud) {
        Producto producto = Optional.ofNullable(detalle)
                .map(SolicitudMovimientoDetalle::getLote)
                .map(LoteProducto::getProducto)
                .orElseGet(() -> solicitud != null ? solicitud.getProducto() : null);
        return producto != null ? producto.getNombre() : "Insumo";
    }

    private void validarAutenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Token inválido o no proporcionado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudesPorOrdenDTO> listGroupByOrden(List<EstadoSolicitudMovimiento> estados,
                                                         LocalDateTime desde,
                                                         LocalDateTime hasta,
                                                         Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        List<EstadoSolicitudMovimiento> filtros = (estados != null && !estados.isEmpty())
                ? estados
                : List.of(EstadoSolicitudMovimiento.PENDIENTE);
        LocalDateTime inicio = desde;
        LocalDateTime fin = hasta;
        boolean filtrarOpAbierta = filtros.contains(EstadoSolicitudMovimiento.AUTORIZADA);
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(
                null,
                filtros,
                inicio,
                fin,
                filtrarOpAbierta,
                ESTADOS_OP_CERRADOS
        );
        if (filtrarOpAbierta) {
            solicitudes = solicitudes.stream()
                    .filter(s -> s.getOrdenProduccion() == null
                            || s.getOrdenProduccion().getEstado() == null
                            || !ESTADOS_OP_CERRADOS.contains(s.getOrdenProduccion().getEstado()))
                    .toList();
        }
        Map<Long, List<SolicitudMovimiento>> agrupadas = new LinkedHashMap<>();
        for (SolicitudMovimiento s : solicitudes) {
            if (s.getOrdenProduccion() == null) continue;
            Long key = s.getOrdenProduccion().getId();
            agrupadas.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        List<SolicitudesPorOrdenDTO> dtos = agrupadas.values().stream()
                .map(list -> {
                    OrdenProduccion op = list.get(0).getOrdenProduccion();
                    List<SolicitudMovimientoItemDTO> items = list.stream()
                            .flatMap(s -> s.getDetalles().stream().map(det -> toItemDTO(s, det)))
                            .collect(Collectors.toList());
                    String estadoAgregado = calcularEstadoAgregado(items);
                    return SolicitudesPorOrdenDTO.builder()
                            .ordenProduccionId(op.getId())
                            .codigoOrden(op.getCodigoOrden())
                            .estadoAgregado(estadoAgregado)
                            .solicitanteOrden(op.getResponsable() != null ? op.getResponsable().getNombreCompleto() : null)
                            .fechaOrden(op.getFechaInicio())
                            .items(items)
                            .itemsCount(items.size())
                            .build();
                })
                .collect(Collectors.toList());

        Sort.Order order = pageable.getSort().isEmpty()
                ? Sort.Order.desc("fechaOrden")
                : pageable.getSort().iterator().next();
        Comparator<SolicitudesPorOrdenDTO> comparator;
        if ("codigoOrden".equals(order.getProperty()) || "op".equals(order.getProperty())) {
            comparator = Comparator.comparing(SolicitudesPorOrdenDTO::getCodigoOrden,
                    Comparator.nullsLast(String::compareTo));
        } else {
            comparator = Comparator.comparing(SolicitudesPorOrdenDTO::getFechaOrden,
                    Comparator.nullsLast(LocalDateTime::compareTo));
        }
        if (order.getDirection().isDescending()) {
            comparator = comparator.reversed();
        }
        dtos.sort(comparator);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<SolicitudesPorOrdenDTO> contenido = start > end ? Collections.emptyList() : dtos.subList(start, end);
        return new PageImpl<>(contenido, pageable, dtos.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudesPorOrdenDTO obtenerPorOrden(Long ordenId) {
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(
                ordenId,
                null,
                null,
                null,
                false,
                ESTADOS_OP_CERRADOS
        );
        if (solicitudes.isEmpty()) {
            throw new NoSuchElementException("No se encontraron solicitudes para la orden");
        }
        OrdenProduccion op = solicitudes.get(0).getOrdenProduccion();
        List<SolicitudMovimientoItemDTO> items = solicitudes.stream()
                .flatMap(s -> s.getDetalles().stream().map(det -> toItemDTO(s, det)))
                .collect(Collectors.toList());
        String estadoAgregado = calcularEstadoAgregado(items);
        return SolicitudesPorOrdenDTO.builder()
                .ordenProduccionId(op.getId())
                .codigoOrden(op.getCodigoOrden())
                .estadoAgregado(estadoAgregado)
                .solicitanteOrden(op.getResponsable() != null ? op.getResponsable().getNombreCompleto() : null)
                .fechaOrden(op.getFechaInicio())
                .items(items)
                .itemsCount(items.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PicklistDTO generarPicklist(Long ordenId, boolean incluirAprobadas) {
        List<EstadoSolicitudMovimiento> estados = incluirAprobadas ? null : List.of(EstadoSolicitudMovimiento.PENDIENTE);
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(
                ordenId,
                estados,
                null,
                null,
                false,
                ESTADOS_OP_CERRADOS
        );
        if (solicitudes.isEmpty()) {
            throw new NoSuchElementException("No se encontraron solicitudes para la orden");
        }
        OrdenProduccion op = solicitudes.get(0).getOrdenProduccion();
        String codigoOrden = op != null ? op.getCodigoOrden() : String.valueOf(ordenId);
        LocalDate fechaOrden = op != null && op.getFechaInicio() != null ? op.getFechaInicio().toLocalDate() : null;
        String solicitante = op != null && op.getResponsable() != null
                ? op.getResponsable().getNombreCompleto()
                : (solicitudes.get(0).getUsuarioSolicitante() != null
                ? solicitudes.get(0).getUsuarioSolicitante().getNombreCompleto()
                : null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36f, 36f, 36f, 36f);

            Table header = new Table(new float[]{60f, 40f}).useAllAvailableWidth();
            Cell left = new Cell().setBorder(Border.NO_BORDER);
            left.add(new Paragraph()
                    .add(new Text("Picklist OP: ").setBold().setFontSize(11))
                    .add(new Text(codigoOrden != null ? codigoOrden : "-").setFontSize(10)));
            left.add(new Paragraph()
                    .add(new Text("Fecha OP: ").setBold().setFontSize(11))
                    .add(new Text(fechaOrden != null ? fechaOrden.toString() : "-").setFontSize(10)));
            Cell right = new Cell().setBorder(Border.NO_BORDER);
            right.add(new Paragraph()
                    .add(new Text("Solicitante: ").setBold().setFontSize(11))
                    .add(new Text(solicitante != null ? solicitante : "-").setFontSize(10)));
            header.addCell(left);
            header.addCell(right);
            header.setMarginBottom(10f);
            document.add(header);

            float[] widths = {22f, 16f, 10f, 8f, 14f, 14f, 14f, 14f, 24f};
            Table table = new Table(widths).useAllAvailableWidth();
            String[] headers = {"Producto", "Lote", "Cant.", "UM", "Alm. Origen", "Ubic. Origen", "Alm. Destino", "Ubic. Destino", "Observaciones"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFontSize(9).setBold())
                        .setBackgroundColor(new DeviceRgb(0xF2, 0xF2, 0xF2))
                        .setPadding(6)
                        .setTextAlignment(TextAlignment.LEFT));
            }

            List<PicklistItem> items = buildPicklistItems(solicitudes);

            int index = 0;
            for (PicklistItem item : items) {
                index++;
                boolean zebra = index % 2 == 0;
                DeviceRgb zebraColor = new DeviceRgb(0xFA, 0xFA, 0xFA);

                String[] valores = {
                        item.producto(),
                        item.lote(),
                        item.cantidad(),
                        item.unidadMedida(),
                        item.almacenOrigen(),
                        item.ubicacionOrigen(),
                        item.almacenDestino(),
                        item.ubicacionDestino(),
                        item.observaciones()
                };
                for (int i = 0; i < valores.length; i++) {
                    Cell cell = new Cell()
                            .add(new Paragraph(valores[i]).setFontSize(9))
                            .setPadding(5)
                            .setTextAlignment(i == 2 ? TextAlignment.RIGHT : TextAlignment.LEFT);
                    if (zebra) {
                        cell.setBackgroundColor(zebraColor);
                    }
                    table.addCell(cell);
                }
            }
            document.add(table);

            Table firmas = new Table(new float[]{25f, 25f, 25f, 25f})
                    .useAllAvailableWidth()
                    .setMarginTop(20f);
            for (int i = 0; i < 4; i++) {
                firmas.addCell(new Cell().add(new Paragraph("____________________"))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            String[] labels = {"Alistó", "Verificó", "Entregó", "Recibió"};
            for (String l : labels) {
                firmas.addCell(new Cell().add(new Paragraph(l).setFontSize(9))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            document.add(firmas);

            document.close();
            return new PicklistDTO(codigoOrden, out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    List<PicklistItem> buildPicklistItems(List<SolicitudMovimiento> solicitudes) {
        List<PicklistItem> items = new ArrayList<>();
        Map<PicklistKey, String> ubicaciones = cargarUbicacionesFisicas(solicitudes);

        for (SolicitudMovimiento solicitud : solicitudes) {
            List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles())
                    .orElseGet(Collections::emptyList);

            if (!detalles.isEmpty()) {
                for (SolicitudMovimientoDetalle detalle : detalles) {
                    Almacen origen = detalle.getAlmacenOrigen() != null ? detalle.getAlmacenOrigen() : solicitud.getAlmacenOrigen();
                    Almacen destino = detalle.getAlmacenDestino() != null ? detalle.getAlmacenDestino() : solicitud.getAlmacenDestino();

                    String producto = solicitud.getProducto() != null ? solicitud.getProducto().getNombre() : "-";
                    String lote = detalle.getLote() != null ? detalle.getLote().getCodigoLote() : "-";
                    String cantidad = detalle.getCantidad() != null ? String.format(Locale.US, "%,.3f", detalle.getCantidad()) : "-";
                    String um = solicitud.getProducto() != null && solicitud.getProducto().getUnidadMedida() != null
                            ? solicitud.getProducto().getUnidadMedida().getNombre()
                            : "-";
                    String almOrigen = origen != null ? origen.getNombre() : "-";
                    String ubicOrigen = resolverUbicacionFisica(ubicaciones, solicitud, origen,
                            detalle != null ? detalle.getLote() : null);
                    String almDestino = destino != null ? destino.getNombre() : "-";
                    String ubicDestino = "-";
                    String obs = solicitud.getObservaciones() != null ? solicitud.getObservaciones() : "-";

                    items.add(new PicklistItem(
                            producto,
                            lote,
                            cantidad,
                            um,
                            almOrigen,
                            ubicOrigen,
                            almDestino,
                            ubicDestino,
                            obs
                    ));
                }
                continue;
            }

            String producto = solicitud.getProducto() != null ? solicitud.getProducto().getNombre() : "-";
            String lote = solicitud.getLote() != null ? solicitud.getLote().getCodigoLote() : "-";
            String cantidad = solicitud.getCantidad() != null ? String.format(Locale.US, "%,.3f", solicitud.getCantidad()) : "-";
            String um = solicitud.getProducto() != null && solicitud.getProducto().getUnidadMedida() != null
                    ? solicitud.getProducto().getUnidadMedida().getNombre()
                    : "-";
            String almOrigen = solicitud.getAlmacenOrigen() != null ? solicitud.getAlmacenOrigen().getNombre() : "-";
            String ubicOrigen = resolverUbicacionFisica(ubicaciones, solicitud, solicitud.getAlmacenOrigen(), solicitud.getLote());
            String almDestino = solicitud.getAlmacenDestino() != null ? solicitud.getAlmacenDestino().getNombre() : "-";
            String ubicDestino = "-";
            String obs = solicitud.getObservaciones() != null ? solicitud.getObservaciones() : "-";

            items.add(new PicklistItem(
                    producto,
                    lote,
                    cantidad,
                    um,
                    almOrigen,
                    ubicOrigen,
                    almDestino,
                    ubicDestino,
                    obs
            ));
        }

        return items;
    }

    /**
     * Representa una fila del picklist. Los datos de lote y almacén de origen se toman del detalle
     * porque las solicitudes de producción suelen almacenar esa información a nivel de detalle
     * (multi-lote). Los valores de cabecera se mantienen como respaldo cuando no hay detalles.
     */
    static record PicklistItem(
            String producto,
            String lote,
            String cantidad,
            String unidadMedida,
            String almacenOrigen,
            String ubicacionOrigen,
            String almacenDestino,
            String ubicacionDestino,
            String observaciones
    ) {
    }

    private Map<PicklistKey, String> cargarUbicacionesFisicas(List<SolicitudMovimiento> solicitudes) {
        Set<Integer> productoIds = new HashSet<>();
        Set<Integer> almacenIds = new HashSet<>();
        Set<String> codigosLote = new HashSet<>();

        for (SolicitudMovimiento solicitud : solicitudes) {
            List<SolicitudMovimientoDetalle> detalles = Optional.ofNullable(solicitud.getDetalles())
                    .orElseGet(Collections::emptyList);
            if (!detalles.isEmpty()) {
                for (SolicitudMovimientoDetalle detalle : detalles) {
                    Almacen origen = detalle.getAlmacenOrigen() != null ? detalle.getAlmacenOrigen() : solicitud.getAlmacenOrigen();
                    registrarLlaveUbicacion(productoIds, almacenIds, codigosLote, solicitud, origen,
                            detalle != null ? detalle.getLote() : null);
                }
                continue;
            }
            registrarLlaveUbicacion(productoIds, almacenIds, codigosLote, solicitud, solicitud.getAlmacenOrigen(),
                    solicitud.getLote());
        }

        if (productoIds.isEmpty() || almacenIds.isEmpty() || codigosLote.isEmpty()) {
            return Collections.emptyMap();
        }

        List<LoteUbicacionPicklistProjection> ubicaciones = loteRepository.findUbicacionesFisicasPicklist(
                productoIds,
                almacenIds,
                codigosLote
        );
        Map<PicklistKey, String> resultado = new HashMap<>();
        for (LoteUbicacionPicklistProjection ubicacion : ubicaciones) {
            PicklistKey key = new PicklistKey(
                    ubicacion.getProductoId(),
                    ubicacion.getAlmacenId(),
                    ubicacion.getCodigoLote()
            );
            resultado.putIfAbsent(key, formatearUbicacion(ubicacion.getUbicacionCodigo(),
                    ubicacion.getUbicacionDescripcion()));
        }
        return resultado;
    }

    private void registrarLlaveUbicacion(Set<Integer> productoIds,
                                         Set<Integer> almacenIds,
                                         Set<String> codigosLote,
                                         SolicitudMovimiento solicitud,
                                         Almacen origen,
                                         LoteProducto lote) {
        Integer productoId = solicitud.getProducto() != null ? solicitud.getProducto().getId() : null;
        Integer almacenId = origen != null ? origen.getId() : null;
        String codigoLote = lote != null ? lote.getCodigoLote() : null;
        if (productoId != null && almacenId != null && StringUtils.hasText(codigoLote)) {
            productoIds.add(productoId);
            almacenIds.add(almacenId);
            codigosLote.add(codigoLote);
        }
    }

    private String resolverUbicacionFisica(Map<PicklistKey, String> ubicaciones,
                                           SolicitudMovimiento solicitud,
                                           Almacen origen,
                                           LoteProducto lote) {
        Integer productoId = solicitud.getProducto() != null ? solicitud.getProducto().getId() : null;
        Integer almacenId = origen != null ? origen.getId() : null;
        String codigoLote = lote != null ? lote.getCodigoLote() : null;
        if (productoId == null || almacenId == null || !StringUtils.hasText(codigoLote)) {
            return "-";
        }
        return ubicaciones.getOrDefault(new PicklistKey(productoId, almacenId, codigoLote), "-");
    }

    private String formatearUbicacion(String codigo, String descripcion) {
        if (!StringUtils.hasText(codigo) || !StringUtils.hasText(descripcion)) {
            return "-";
        }
        return codigo + " - " + descripcion;
    }

    private record PicklistKey(Integer productoId, Integer almacenId, String codigoLote) {
    }

    private SolicitudMovimientoItemDTO toItemDTO(SolicitudMovimiento s, SolicitudMovimientoDetalle det) {
        Long almacenOrigenId = Optional.ofNullable(det.getAlmacenOrigen())
                .map(Almacen::getId)
                .map(Integer::longValue)
                .orElseGet(() -> Optional.ofNullable(s.getAlmacenOrigen())
                        .map(Almacen::getId)
                        .map(Integer::longValue)
                        .orElseGet(() -> Optional.ofNullable(det.getLote())
                                .map(LoteProducto::getAlmacen)
                                .map(Almacen::getId)
                                .map(Integer::longValue)
                                .orElse(null)));
        Long almacenDestinoId = Optional.ofNullable(det.getAlmacenDestino())
                .map(Almacen::getId)
                .map(Integer::longValue)
                .orElseGet(() -> Optional.ofNullable(s.getAlmacenDestino())
                        .map(Almacen::getId)
                        .map(Integer::longValue)
                        .orElse(null));
        Almacen almacenOrigen = almacenOrigenId != null
                ? almacenRepository.findById(almacenOrigenId).orElse(null)
                : null;
        Almacen almacenDestino = almacenDestinoId != null
                ? almacenRepository.findById(almacenDestinoId).orElse(null)
                : null;
        String estadoDetalle = det.getEstado() != null
                ? det.getEstado().name()
                : (s.getEstado() != null ? s.getEstado().name() : null);

        return SolicitudMovimientoItemDTO.builder()
                .solicitudId(s.getId())
                .productoId(s.getProducto() != null ? s.getProducto().getId().longValue() : null)
                .nombreProducto(s.getProducto() != null ? s.getProducto().getNombre() : null)
                .loteId(det.getLote() != null ? det.getLote().getId() : null)
                .codigoLote(det.getLote() != null ? det.getLote().getCodigoLote() : null)
                .cantidadSolicitada(det.getCantidad())
                .cantidadAtendida(det.getCantidadAtendida())
                .unidadMedida(s.getProducto() != null && s.getProducto().getUnidadMedida() != null ? s.getProducto().getUnidadMedida().getNombre() : null)
                .almacenOrigenId(almacenOrigenId)
                .nombreAlmacenOrigen(almacenOrigen != null ? almacenOrigen.getNombre() : null)
                .ubicacionAlmacenOrigen(almacenOrigen != null ? almacenOrigen.getUbicacion() : "-")
                .almacenDestinoId(almacenDestinoId)
                .nombreAlmacenDestino(almacenDestino != null ? almacenDestino.getNombre() : null)
                .ubicacionAlmacenDestino(almacenDestino != null ? almacenDestino.getUbicacion() : "-")
                .motivoMovimientoId(s.getMotivoMovimiento() != null ? s.getMotivoMovimiento().getId() : null)
                .tipoMovimientoDetalleId(s.getTipoMovimientoDetalle() != null ? s.getTipoMovimientoDetalle().getId() : null)
                .estado(estadoDetalle)
                .estadoDetalle(estadoDetalle)
                .fechaSolicitud(s.getFechaSolicitud())
                .usuarioSolicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : null)
                .observaciones(s.getObservaciones())
                .build();
    }

    private String calcularEstadoAgregado(List<SolicitudMovimientoItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return "PENDIENTE";
        }
        boolean todosPendientes = items.stream().allMatch(i -> "PENDIENTE".equals(i.getEstado()));
        boolean todosAutorizados = items.stream().allMatch(i -> "AUTORIZADA".equals(i.getEstado()));
        if (todosPendientes) {
            return "PENDIENTE";
        }
        if (todosAutorizados) {
            return "AUTORIZADA";
        }
        return "MIXTO";
    }

    /**
     * Convierte una {@link SolicitudMovimiento} en su DTO de respuesta asegurando que el frontend
     * reciba siempre los identificadores necesarios para precargar el formulario de movimientos.
     * <p>
     * Las solicitudes ligadas a producción suelen persistir la información de lote y almacenes en
     * el primer detalle (multi-lote) y dejar vacía la cabecera. Por ello se toma ese detalle como
     * fuente de verdad cuando los campos principales vienen nulos.
     * </p>
     */
    private SolicitudMovimientoResponseDTO toResponse(SolicitudMovimiento s) {
        SolicitudMovimientoDetalle primerDetalle = Optional.ofNullable(s.getDetalles())
                .filter(detalles -> !detalles.isEmpty())
                .map(detalles -> detalles.get(0))
                .orElse(null);

        LoteProducto loteCabecera = s.getLote() != null ? s.getLote()
                : primerDetalle != null ? primerDetalle.getLote() : null;
        Almacen origenCabecera = s.getAlmacenOrigen() != null ? s.getAlmacenOrigen()
                : primerDetalle != null ? primerDetalle.getAlmacenOrigen() : null;
        Almacen destinoCabecera = s.getAlmacenDestino() != null ? s.getAlmacenDestino()
                : primerDetalle != null ? primerDetalle.getAlmacenDestino() : null;

        return SolicitudMovimientoResponseDTO.builder()
                .id(s.getId())
                .tipoMovimiento(s.getTipoMovimiento())
                .productoId(s.getProducto() != null ? s.getProducto().getId() : null)
                .nombreProducto(s.getProducto() != null ? s.getProducto().getNombre() : null)
                .codigoSku(s.getProducto() != null ? s.getProducto().getCodigoSku() : null)
                .loteProductoId(loteCabecera != null ? loteCabecera.getId() : null)
                .nombreLote(loteCabecera != null ? loteCabecera.getCodigoLote() : null)
                .cantidad(s.getCantidad())
                .almacenOrigenId(origenCabecera != null ? origenCabecera.getId() : null)
                .nombreAlmacenOrigen(origenCabecera != null ? origenCabecera.getNombre() : null)
                .almacenDestinoId(destinoCabecera != null ? destinoCabecera.getId() : null)
                .nombreAlmacenDestino(destinoCabecera != null ? destinoCabecera.getNombre() : null)
                .ordenProduccionId(s.getOrdenProduccion() != null ? s.getOrdenProduccion().getId() : null)
                .codigoOrdenProduccion(s.getOrdenProduccion() != null ? s.getOrdenProduccion().getCodigoOrden() : null)
                .codigoOrden(s.getOrdenProduccion() != null ? s.getOrdenProduccion().getCodigoOrden() : null)
                .motivoMovimientoId(s.getMotivoMovimiento() != null ? s.getMotivoMovimiento().getId() : null)
                .tipoMovimientoDetalleId(s.getTipoMovimientoDetalle() != null ? s.getTipoMovimientoDetalle().getId() : null)
                .nombreSolicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : null)
                .nombreResponsable(s.getUsuarioResponsable() != null ? s.getUsuarioResponsable().getNombreCompleto() : null)
                .estado(s.getEstado())
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaResolucion(s.getFechaResolucion())
                .observaciones(s.getObservaciones())
                .detalles(s.getDetalles() != null ? s.getDetalles().stream()
                        .map(det -> SolicitudMovimientoDetalleDTO.builder()
                                .loteId(det.getLote() != null ? det.getLote().getId() : null)
                                .codigoLote(det.getLote() != null ? det.getLote().getCodigoLote() : null)
                                .cantidad(det.getCantidad())
                                .cantidadAtendida(det.getCantidadAtendida())
                                .estado(det.getEstado())
                                .almacenOrigenId(det.getAlmacenOrigen() != null ? det.getAlmacenOrigen().getId() : null)
                                .nombreAlmacenOrigen(det.getAlmacenOrigen() != null ? det.getAlmacenOrigen().getNombre() : null)
                                .almacenDestinoId(det.getAlmacenDestino() != null ? det.getAlmacenDestino().getId() : null)
                                .nombreAlmacenDestino(det.getAlmacenDestino() != null ? det.getAlmacenDestino().getNombre() : null)
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private SolicitudMovimientoListadoDTO toListadoDTO(SolicitudMovimiento s) {
        String op;
        if (s.getOrdenProduccion() != null) {
            op = s.getOrdenProduccion().getCodigoOrden();
        } else if (s.getId() != null) {
            op = s.getId().toString();
        } else {
            op = "";
        }
        return SolicitudMovimientoListadoDTO.builder()
                .id(s.getId())
                .op(op)
                .fechaSolicitud(s.getFechaSolicitud())
                .items(0)
                .itemsCount(0)
                .estado(s.getEstado() != null ? s.getEstado().name() : "")
                .solicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : "")
                .build();
    }
}
