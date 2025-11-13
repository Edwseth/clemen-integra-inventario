package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.mapper.RetencionLoteMapper;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
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
    private final UsuarioRepository usuarioRepository;
    private final RetencionLoteMapper mapper;

    public Page<RetencionLoteDTO> listar(EstadoRetencion estado, Pageable pageable) {
        Page<RetencionLote> page = (estado != null)
                ? repository.findByEstado(estado, pageable)
                : repository.findAll(pageable);
        return page.map(mapper::toDTO);
    }

    @Transactional
    public RetencionLoteDTO crear(RetencionLoteDTO dto) {
        LoteProducto lote = loteRepository.findById(dto.getLoteId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteId()));
        Usuario user = usuarioRepository.findById(dto.getAprobadoPorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getAprobadoPorId()));
        RetencionLote entity = mapper.toEntity(dto, lote, user);
        return mapper.toDTO(repository.save(entity));
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
        return mapper.toDTO(repository.save(existing));
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

        asegurarEstadoRetenido(lote);
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
        if (!esJefeOSuper()) {
            throw new AccessDeniedException("Solo Jefe de Calidad o Super Admin pueden levantar retenciones");
        }
        if (retencionId == null) {
            throw new IllegalArgumentException("Retención requerida");
        }
        RetencionLote retencion = repository.findById(retencionId)
                .orElseThrow(() -> new NoSuchElementException("Retención no encontrada con ID: " + retencionId));
        if (retencion.getEstado() == EstadoRetencion.LIBERADO) {
            return retencion;
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
        return repository.save(retencion);
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

    private void asegurarEstadoRetenido(LoteProducto lote) {
        if (lote != null && lote.getEstado() != EstadoLote.RETENIDO) {
            lote.setEstado(EstadoLote.RETENIDO);
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

