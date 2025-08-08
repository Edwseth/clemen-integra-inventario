package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.SolicitudMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.EstadoSolicitudMovimiento;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
