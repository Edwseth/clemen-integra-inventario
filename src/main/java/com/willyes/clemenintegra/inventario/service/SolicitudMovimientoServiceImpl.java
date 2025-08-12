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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private final MovimientoInventarioService movimientoService;

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO registrarSolicitud(SolicitudMovimientoRequestDTO dto) {
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

        SolicitudMovimiento solicitud = SolicitudMovimiento.builder()
                .tipoMovimiento(dto.getTipoMovimiento())
                .producto(producto)
                .lote(lote)
                .cantidad(dto.getCantidad())
                .almacenOrigen(origen)
                .almacenDestino(destino)
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
    // CODEx: servicio que alimenta el listado de solicitudes
    public List<SolicitudMovimientoResponseDTO> listarSolicitudes(EstadoSolicitudMovimiento estado, LocalDateTime desde, LocalDateTime hasta) {
        List<SolicitudMovimiento> lista;
        if (estado != null) {
            lista = repository.findByEstado(estado);
        } else {
            lista = repository.findAll();
        }
        return lista.stream()
                .filter(s -> desde == null || !s.getFechaSolicitud().isBefore(desde))
                .filter(s -> hasta == null || !s.getFechaSolicitud().isAfter(hasta))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO aprobarSolicitud(Long id, Long responsableId) {
        SolicitudMovimiento solicitud = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudMovimiento.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue procesada");
        }
        Usuario responsable = usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        solicitud.setUsuarioResponsable(responsable);
        solicitud.setEstado(EstadoSolicitudMovimiento.APROBADO);
        solicitud.setFechaResolucion(LocalDateTime.now());

        // CODEx: aprobar actualmente crea un movimiento simple sin lógica FIFO ni prellenado
        movimientoService.registrarMovimiento(
                new com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO(
                        null,
                        solicitud.getCantidad(),
                        solicitud.getTipoMovimiento(),
                        null,
                        solicitud.getObservaciones(),
                        solicitud.getProducto().getId().intValue(),
                        solicitud.getLote() != null ? solicitud.getLote().getId() : null,
                        solicitud.getAlmacenOrigen() != null ? solicitud.getAlmacenOrigen().getId().intValue() : null,
                        solicitud.getAlmacenDestino() != null ? solicitud.getAlmacenDestino().getId().intValue() : null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        responsableId,
                        null,
                        null,
                        null
                )
        );

        SolicitudMovimiento actualizada = repository.save(solicitud);
        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public SolicitudMovimientoResponseDTO rechazarSolicitud(Long id, Long responsableId, String observaciones) {
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
    @Transactional(readOnly = true)
    public Page<SolicitudesPorOrdenDTO> listGroupByOrden(EstadoSolicitudMovimiento estado,
                                                         LocalDateTime desde,
                                                         LocalDateTime hasta,
                                                         Pageable pageable) {
        List<SolicitudMovimiento> solicitudes = repository.findWithDetalles(null, estado, desde, hasta);
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
                    return SolicitudesPorOrdenDTO.builder()
                            .ordenProduccionId(op.getId())
                            .codigoOrden(op.getCodigoOrden())
                            .fechaCreacionOrden(op.getFechaInicio())
                            .solicitanteOrden(op.getResponsable() != null ? op.getResponsable().getNombreCompleto() : null)
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

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            float margin = 40f;
            float y = page.getMediaBox().getHeight() - margin;

            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.newLineAtOffset(margin, y);
            content.showText("Picklist OP: " + codigoOrden);
            content.endText();

            y -= 20;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(margin, y);
            content.showText("Fecha: " + LocalDateTime.now().toLocalDate());
            content.endText();

            y -= 20;
            String solicitante = solicitudes.get(0).getUsuarioSolicitante() != null ?
                    solicitudes.get(0).getUsuarioSolicitante().getNombreCompleto() : "";
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 12);
            content.newLineAtOffset(margin, y);
            content.showText("Solicitante: " + solicitante);
            content.endText();

            y -= 30;
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            String[] headers = {"Producto", "Lote", "Cantidad", "UM", "Alm. Origen", "Alm. Destino", "Observaciones"};
            float[] widths = {80, 60, 60, 40, 80, 80, 140};
            float x;
            x = margin;
            for (int i = 0; i < headers.length; i++) {
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(headers[i]);
                content.endText();
                x += widths[i];
            }

            y -= 15;
            content.setFont(PDType1Font.HELVETICA, 9);
            for (SolicitudMovimiento s : solicitudes) {
                x = margin;
                String[] valores = {
                        s.getProducto() != null ? s.getProducto().getNombre() : "",
                        s.getLote() != null ? s.getLote().getCodigoLote() : "",
                        s.getCantidad() != null ? s.getCantidad().toString() : "",
                        s.getProducto() != null && s.getProducto().getUnidadMedida() != null ? s.getProducto().getUnidadMedida().getNombre() : "",
                        s.getAlmacenOrigen() != null ? s.getAlmacenOrigen().getNombre() : "",
                        s.getAlmacenDestino() != null ? s.getAlmacenDestino().getNombre() : "",
                        s.getObservaciones() != null ? s.getObservaciones() : ""
                };
                for (int i = 0; i < valores.length; i++) {
                    content.beginText();
                    content.newLineAtOffset(x, y);
                    content.showText(valores[i]);
                    content.endText();
                    x += widths[i];
                }
                y -= 15;
                if (y <= margin) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                }
            }

            y -= 40;
            content.beginText();
            content.newLineAtOffset(margin, y);
            content.showText("___________________________");
            content.endText();
            content.beginText();
            content.newLineAtOffset(margin + 200, y);
            content.showText("___________________________");
            content.endText();

            content.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
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
                .almacenDestinoId(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getId().longValue() : null)
                .nombreAlmacenDestino(s.getAlmacenDestino() != null ? s.getAlmacenDestino().getNombre() : null)
                .estado(s.getEstado())
                .fechaSolicitud(s.getFechaSolicitud())
                .usuarioSolicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : null)
                .observaciones(s.getObservaciones())
                .build();
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
                .nombreSolicitante(s.getUsuarioSolicitante() != null ? s.getUsuarioSolicitante().getNombreCompleto() : null)
                .nombreResponsable(s.getUsuarioResponsable() != null ? s.getUsuarioResponsable().getNombreCompleto() : null)
                .estado(s.getEstado())
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaResolucion(s.getFechaResolucion())
                .observaciones(s.getObservaciones())
                .build();
    }
}
