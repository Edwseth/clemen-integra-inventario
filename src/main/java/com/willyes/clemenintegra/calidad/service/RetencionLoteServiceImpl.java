package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RetencionLoteServiceImpl implements RetencionLoteService {

    private final RetencionLoteRepository repository;
    private final LoteProductoRepository loteRepository;
    private final AlmacenRepository almacenRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final UsuarioRepository usuarioRepository;
    private final RetencionLoteMapper mapper;
    private final NoConformidadRepository noConformidadRepository;

    public Page<RetencionLoteDTO> listar(EstadoRetencion estado, Pageable pageable) {
        Page<RetencionLote> page = (estado != null)
                ? repository.findByEstado(estado, pageable)
                : repository.findAll(pageable);
        return page.map(mapper::toDTO);
    }

    @Transactional
    public RetencionLoteDTO crear(RetencionLoteDTO dto) {
        if (dto == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA, "Debe enviar el detalle de la retención.");
        }
        if (dto.getLoteId() == null) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_LOTE_REQUERIDO, "Debe seleccionar un lote.");
        }
        if (dto.getMotivo() == null) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_MOTIVO_INVALIDO, "Debe seleccionar un motivo válido.");
        }
        if (dto.getAprobadoPorId() == null) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_APROBADOR_REQUERIDO, "El aprobador es obligatorio.");
        }
        if (dto.getCausa() == null || dto.getCausa().isBlank()) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_CAUSA_REQUERIDA, "La causa de la retención es obligatoria.");
        }

        LoteProducto lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RETENCION_LOTE_NO_ENCONTRADO,
                        "Lote no encontrado con ID: " + dto.getLoteId()));
        if (lote.getEstado() == EstadoLote.RECHAZADO || lote.getEstado() == EstadoLote.VENCIDO) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_ESTADO_NO_PERMITIDO,
                    "El estado del lote no permite retenciones.",
                    java.util.Map.of("loteId", lote.getId(), "estado", lote.getEstado().name()));
        }
        Usuario user = usuarioRepository.findById(dto.getAprobadoPorId())
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RETENCION_APROBADOR_NO_ENCONTRADO,
                        "Usuario no encontrado con ID: " + dto.getAprobadoPorId()));
        RetencionLote entity = RetencionLote.builder()
                .lote(lote)
                .causa(dto.getCausa().trim())
                .fechaRetencion(LocalDateTime.now())
                .fechaLiberacion(dto.getFechaLiberacion())
                .estado(EstadoRetencion.RETENIDO)
                .motivo(dto.getMotivo())
                .noConformidad(dto.getNoConformidadId() != null
                        ? NoConformidad.builder().id(dto.getNoConformidadId()).build()
                        : null)
                .aprobadoPor(user)
                .build();
        RetencionLote guardada = repository.save(entity);
        sincronizarEstadoRetenido(guardada.getLote(), guardada);
        asegurarAlmacenCuarentena(guardada.getLote());
        return mapper.toDTO(guardada);
    }

    @Transactional
    public RetencionLoteDTO actualizar(Long id, RetencionLoteDTO dto) {
        RetencionLote existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Retención no encontrada con ID: " + id));
        LoteProducto lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteId()));
        Usuario user = usuarioRepository.findById(dto.getAprobadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getAprobadoPorId()));
        existing.setLote(lote);
        existing.setCausa(dto.getCausa());
        existing.setFechaRetencion(dto.getFechaRetencion());
        existing.setFechaLiberacion(dto.getFechaLiberacion());
        if (existing.getEstado() != EstadoRetencion.LIBERADO
                && dto.getEstado() == EstadoRetencion.LIBERADO
                && !esJefeOSuper()) {
            throw new AccessDeniedException("Solo Jefe de Calidad o Super Admin pueden liberar retenciones");
        }

        existing.setEstado(dto.getEstado());
        existing.setAprobadoPor(user);
        RetencionLote guardada = repository.save(existing);
        if (guardada.getEstado() == EstadoRetencion.RETENIDO) {
            sincronizarEstadoRetenido(lote, guardada);
        } else if (guardada.getEstado() == EstadoRetencion.LIBERADO) {
            List<RetencionLote> activas = repository.findByLote_IdAndEstado(lote.getId(), EstadoRetencion.RETENIDO);
            if (activas.isEmpty()) {
                actualizarEstadoPostLevantamiento(lote);
            }
        }
        return mapper.toDTO(guardada);
    }

    public RetencionLoteDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Retención no encontrada con ID: " + id));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public RetencionLote retener(Long loteId,
                                 MotivoRetencion motivo,
                                 String descripcion,
                                 NoConformidad noConformidad,
                                 Usuario usuario) {
        return retenerLote(loteId, motivo, descripcion, noConformidad, usuario, false);
    }

    @Override
    @Transactional
    public RetencionLote retenerLote(Long loteId,
                                     MotivoRetencion motivo,
                                     String descripcion,
                                     NoConformidad noConformidad,
                                     Usuario usuario,
                                     boolean moverACuarentena) {
        if (loteId == null) {
            throw new IllegalArgumentException("Lote requerido");
        }
        if (motivo == null) {
            throw new IllegalArgumentException("Motivo de retención requerido");
        }
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("Usuario requerido");
        }

        LoteProducto lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + loteId));
        Usuario aprobadoPor = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuario.getId()));

        Optional<RetencionLote> existente = repository.findFirstByLote_IdAndEstadoAndMotivo(
                loteId, EstadoRetencion.RETENIDO, motivo);

        RetencionLote resultado;
        if (existente.isPresent()) {
            RetencionLote retencion = existente.get();
            if (descripcion != null && !descripcion.isBlank()) {
                retencion.setCausa(descripcion);
            }
            if (noConformidad != null && (retencion.getNoConformidad() == null
                    || (retencion.getNoConformidad().getId() != null
                    && !retencion.getNoConformidad().getId().equals(noConformidad.getId())))) {
                retencion.setNoConformidad(noConformidad);
            }
            resultado = repository.save(retencion);
        } else {
            RetencionLote nueva = RetencionLote.builder()
                    .lote(lote)
                    .causa(descripcion != null && !descripcion.isBlank() ? descripcion : "NC pendiente")
                    .fechaRetencion(LocalDateTime.now())
                    .estado(EstadoRetencion.RETENIDO)
                    .motivo(motivo)
                    .noConformidad(noConformidad)
                    .aprobadoPor(aprobadoPor)
                    .build();
            resultado = repository.save(nueva);
        }

        sincronizarEstadoRetenido(lote, resultado);
        if (moverACuarentena) {
            asegurarAlmacenCuarentena(lote);
        }
        return resultado;
    }

    @Override
    @Transactional
    public RetencionLote asegurarRetencionNoConformidad(LoteProducto lote,
                                                        String descripcion,
                                                        NoConformidad noConformidad,
                                                        Usuario usuario) {
        if (lote == null || lote.getId() == null) {
            throw new IllegalArgumentException("Lote requerido");
        }
        return retener(lote.getId(), MotivoRetencion.NO_CONFORMIDAD, descripcion, noConformidad, usuario);
    }

    @Override
    @Transactional
    public RetencionLote levantar(Long retencionId, Usuario usuario) {
        return levantarRetencion(retencionId, usuario);
    }

    @Override
    @Transactional
    public RetencionLote levantarRetencion(Long retencionId, Usuario usuario) {
        if (!esJefeOSuper()) {
            throw new AccessDeniedException("Solo Jefe de Calidad o Super Admin pueden levantar retenciones");
        }
        if (retencionId == null) {
            throw new IllegalArgumentException("Retención requerida");
        }
        RetencionLote retencion = repository.findById(retencionId)
                .orElseThrow(() -> new CustomBusinessException(ApiErrorCode.RETENCION_NO_ENCONTRADA,
                        "Retención no encontrada con ID: " + retencionId));
        if (retencion.getEstado() != EstadoRetencion.RETENIDO) {
            throw new CustomBusinessException(ApiErrorCode.RETENCION_NO_ACTIVA,
                    "La retención no está activa o ya fue levantada.",
                    java.util.Map.of("retencionId", retencionId,
                            "estado", retencion.getEstado() != null ? retencion.getEstado().name() : null));
        }

        if (retencion.getMotivo() == MotivoRetencion.NO_CONFORMIDAD) {
            List<NoConformidad> noConformidades = new java.util.ArrayList<>();
            if (retencion.getNoConformidad() != null && retencion.getNoConformidad().getId() != null) {
                noConformidadRepository.findById(retencion.getNoConformidad().getId())
                        .ifPresent(noConformidades::add);
            }
            if (retencion.getLote() != null && retencion.getLote().getId() != null) {
                for (NoConformidad nc : noConformidadRepository.findByLote_Id(retencion.getLote().getId())) {
                    if (noConformidades.stream().noneMatch(existing -> existing.getId().equals(nc.getId()))) {
                        noConformidades.add(nc);
                    }
                }
            }
            boolean ncAbierta = noConformidades.stream()
                    .anyMatch(nc -> nc.getEstado() == EstadoNoConformidad.ABIERTA);
            if (ncAbierta) {
                throw new CustomBusinessException(ApiErrorCode.RETENCION_NC_NO_CERRADA,
                        "No puede levantarse la retención porque la No Conformidad asociada está abierta.",
                        java.util.Map.of("retencionId", retencionId,
                                "loteId", retencion.getLote() != null ? retencion.getLote().getId() : null));
            }
        }

        Usuario aprobador = retencion.getAprobadoPor();
        if (usuario != null && usuario.getId() != null) {
            aprobador = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuario.getId()));
        }
        if (aprobador != null) {
            retencion.setAprobadoPor(aprobador);
        }
        retencion.setEstado(EstadoRetencion.LIBERADO);
        retencion.setFechaLiberacion(LocalDateTime.now());
        RetencionLote guardada = repository.save(retencion);
        LoteProducto lote = guardada.getLote();
        if (lote != null) {
            List<RetencionLote> activas = repository.findByLote_IdAndEstado(lote.getId(), EstadoRetencion.RETENIDO);
            if (activas.isEmpty()) {
                actualizarEstadoPostLevantamiento(lote);
            }
        }
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetencionLote> obtenerActivaPorLote(Long loteId) {
        if (loteId == null) {
            return Optional.empty();
        }
        return repository.findFirstByLote_IdAndEstadoAndMotivo(loteId, EstadoRetencion.RETENIDO, MotivoRetencion.NO_CONFORMIDAD);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetencionLote> obtenerRetencionesActivas(Long loteId) {
        if (loteId == null) {
            return List.of();
        }
        return repository.findByLote_IdAndEstado(loteId, EstadoRetencion.RETENIDO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetencionLote> obtenerRetencionesPorLote(Long loteId) {
        if (loteId == null) {
            return List.of();
        }
        return repository.findByLote_Id(loteId);
    }

    private void sincronizarEstadoRetenido(LoteProducto lote, RetencionLote retencion) {
        if (lote == null || lote.getEstado() == null) {
            return;
        }
        if (lote.getEstado() == EstadoLote.RECHAZADO || lote.getEstado() == EstadoLote.VENCIDO) {
            return;
        }
        if (lote.getEstado() != EstadoLote.RETENIDO && retencion != null
                && retencion.getEstado() == EstadoRetencion.RETENIDO) {
            lote.setEstado(EstadoLote.RETENIDO);
            loteRepository.save(lote);
        }
    }

    private void actualizarEstadoPostLevantamiento(LoteProducto lote) {
        if (lote == null || lote.getEstado() == null) {
            return;
        }
        if (lote.getEstado() == EstadoLote.RECHAZADO || lote.getEstado() == EstadoLote.VENCIDO) {
            return;
        }
        if (lote.getEstado() != EstadoLote.RETENIDO) {
            return;
        }
        lote.setEstado(EstadoLote.EN_CUARENTENA);
        asegurarAlmacenCuarentena(lote);
        loteRepository.save(lote);
    }

    private void asegurarAlmacenCuarentena(LoteProducto lote) {
        if (lote == null) {
            return;
        }
        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        if (cuarentenaId == null) {
            return;
        }
        if (lote.getAlmacen() == null || !cuarentenaId.equals(lote.getAlmacen().getId().longValue())) {
            lote.setAlmacen(almacenRepository.findById(cuarentenaId)
                    .orElseGet(() -> new com.willyes.clemenintegra.inventario.model.Almacen(Math.toIntExact(cuarentenaId))));
            loteRepository.save(lote);
        }
    }

    private boolean esJefeOSuper() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return tieneAlgunaAuthority(authentication, "ROL_JEFE_CALIDAD", "ROL_SUPER_ADMIN");
    }

    private boolean tieneAlgunaAuthority(Authentication authentication, String... authorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        java.util.Set<String> requeridas = java.util.Set.of(authorities);
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (requeridas.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
