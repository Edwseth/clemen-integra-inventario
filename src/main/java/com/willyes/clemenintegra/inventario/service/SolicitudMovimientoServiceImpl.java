package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.*;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolicitudMovimientoServiceImpl implements SolicitudMovimientoService {

    private final SolicitudMovimientoRepository repository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;
    private final AlmacenRepository almacenRepository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

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
                .estado(EstadoSolicitudMovimiento.PENDIENTE)
                .build();

        SolicitudMovimiento guardada = repository.save(solicitud);
        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    // servicio que alimenta el listado de solicitudes con paginación y filtros
    public Page<SolicitudMovimientoListadoDTO> listarSolicitudes(EstadoSolicitudMovimiento estado,
                                                                 String busqueda,
                                                                 Long almacenOrigenId,
                                                                 Long almacenDestinoId,
                                                                 LocalDateTime desde,
                                                                 LocalDateTime hasta,
                                                                 Pageable pageable) {
        Specification<SolicitudMovimiento> spec = Specification.where(null);

        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (almacenOrigenId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("almacenOrigen", javax.persistence.criteria.JoinType.LEFT).get("id"), almacenOrigenId));
        }
        if (almacenDestinoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("almacenDestino", javax.persistence.criteria.JoinType.LEFT).get("id"), almacenDestinoId));
        }
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaSolicitud"), hasta));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            String like = "%" + busqueda.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                var productoJoin = root.join("producto", javax.persistence.criteria.JoinType.LEFT);
                var solicitanteJoin = root.join("usuarioSolicitante", javax.persistence.criteria.JoinType.LEFT);
                var ordenJoin = root.join("ordenProduccion", javax.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(productoJoin.get("nombre")), like),
                        cb.like(cb.lower(solicitanteJoin.get("nombreCompleto")), like),
                        cb.like(cb.lower(ordenJoin.get("codigoOrden")), like)
                );
            });
        }

        return repository.findAll(spec, pageable)
                .map(this::toListadoDTO);
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId) {
        validarAutenticacion();
        SolicitudMovimiento solicitud = repository.findById(id)
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
        SolicitudMovimiento actualizada = repository.save(solicitud);
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

    private void validarAutenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Token inválido o no proporcionado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudesPorOrdenDTO> listGroupByOrden(EstadoSolicitudMovimiento estado,
                                                         LocalDateTime desde,
                                                         LocalDateTime hasta,
                                                         Pageable pageable) {
        EstadoSolicitudMovimiento filtro = estado != null ? estado : EstadoSolicitudMovimiento.PENDIENTE;
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(null, filtro, desde, hasta);
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
                            .map(this::toItemDTO)
                            .collect(Collectors.toList());
                    String estadoAgregado = calcularEstadoAgregado(items);
                    return SolicitudesPorOrdenDTO.builder()
                            .ordenProduccionId(op.getId())
                            .codigoOrden(op.getCodigoOrden())
                            .estadoAgregado(estadoAgregado)
                            .solicitanteOrden(op.getResponsable() != null ? op.getResponsable().getNombreCompleto() : null)
                            .fechaOrden(op.getFechaInicio())
                            .items(items)
                            .build();
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<SolicitudesPorOrdenDTO> contenido = start > end ? Collections.emptyList() : dtos.subList(start, end);
        return new PageImpl<>(contenido, pageable, dtos.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PicklistDTO generarPicklist(Long ordenId, boolean incluirAprobadas) {
        EstadoSolicitudMovimiento estado = incluirAprobadas ? null : EstadoSolicitudMovimiento.PENDIENTE;
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(ordenId, estado, null, null);
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

            int index = 0;
            for (SolicitudMovimiento s : solicitudes) {
                index++;
                boolean zebra = index % 2 == 0;
                DeviceRgb zebraColor = new DeviceRgb(0xFA, 0xFA, 0xFA);

                String producto = s.getProducto() != null ? s.getProducto().getNombre() : "-";
                String lote = s.getLote() != null ? s.getLote().getCodigoLote() : "-";
                String cantidad = s.getCantidad() != null ? String.format(Locale.US, "%,.3f", s.getCantidad()) : "-";
                String um = s.getProducto() != null && s.getProducto().getUnidadMedida() != null ? s.getProducto().getUnidadMedida().getNombre() : "-";
                String almOrig = s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getNombre() : "-";
                String ubicOrig = s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getUbicacion() : "-";
                String almDest = s.getAlmacenDestino() != null ? s.getAlmacenDestino().getNombre() : "-";
                String ubicDest = s.getAlmacenDestino() != null ? s.getAlmacenDestino().getUbicacion() : "-";
                String obs = s.getObservaciones() != null ? s.getObservaciones() : "-";

                String[] valores = {producto, lote, cantidad, um, almOrig, ubicOrig, almDest, ubicDest, obs};
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

    private SolicitudMovimientoItemDTO toItemDTO(SolicitudMovimiento s) {
        return SolicitudMovimientoItemDTO.builder()
                .solicitudId(s.getId())
                .productoId(s.getProducto() != null ? s.getProducto().getId().longValue() : null)
                .nombreProducto(s.getProducto() != null ? s.getProducto().getNombre() : null)
                .codigoLote(s.getLote() != null ? s.getLote().getCodigoLote() : null)
                .cantidadSolicitada(s.getCantidad())
                .unidadMedida(s.getProducto() != null && s.getProducto().getUnidadMedida() != null ? s.getProducto().getUnidadMedida().getNombre() : null)
                .almacenOrigenId(s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getId().longValue() : null)
                .nombreAlmacenOrigen(s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getNombre() : null)
                .ubicacionAlmacenOrigen(s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getUbicacion() : "-")
                .almacenDestinoId(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getId().longValue() : null)
                .nombreAlmacenDestino(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getNombre() : null)
                .ubicacionAlmacenDestino(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getUbicacion() : "-")
                .motivoMovimientoId(s.getMotivoMovimiento() != null ? s.getMotivoMovimiento().getId() : null)
                .tipoMovimientoDetalleId(s.getTipoMovimientoDetalle() != null ? s.getTipoMovimientoDetalle().getId() : null)
                .estado(s.getEstado() != null ? s.getEstado().name() : null)
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

    private SolicitudMovimientoResponseDTO toResponse(SolicitudMovimiento s) {
        return SolicitudMovimientoResponseDTO.builder()
                .id(s.getId())
                .tipoMovimiento(s.getTipoMovimiento())
                .productoId(s.getProducto() != null ? s.getProducto().getId() : null)
                .nombreProducto(s.getProducto() != null ? s.getProducto().getNombre() : null)
                .loteProductoId(s.getLote() != null ? s.getLote().getId() : null)
                .nombreLote(s.getLote() != null ? s.getLote().getCodigoLote() : null)
                .cantidad(s.getCantidad())
                .almacenOrigenId(s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getId() : null)
                .nombreAlmacenOrigen(s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getNombre() : null)
                .almacenDestinoId(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getId() : null)
                .nombreAlmacenDestino(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getNombre() : null)
                .ordenProduccionId(s.getOrdenProduccion() != null ? s.getOrdenProduccion().getId() : null)
                .motivoMovimientoId(s.getMotivoMovimiento() != null ? s.getMotivoMovimiento().getId() : null)
                .tipoMovimientoDetalleId(s.getTipoMovimientoDetalle() != null ? s.getTipoMovimientoDetalle().getId() : null)
                .nombreSolicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : null)
                .nombreResponsable(s.getUsuarioResponsable() != null ? s.getUsuarioResponsable().getNombreCompleto() : null)
                .estado(s.getEstado())
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaResolucion(s.getFechaResolucion())
                .observaciones(s.getObservaciones())
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
                .items(1)
                .estado(s.getEstado() != null ? s.getEstado().name() : "")
                .solicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : "")
                .build();
    }
}
