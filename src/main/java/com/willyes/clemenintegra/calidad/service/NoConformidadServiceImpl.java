package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.mapper.NoConformidadMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.calidad.repository.NoConformidadRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoConformidadServiceImpl implements NoConformidadService {

    private final NoConformidadRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final NoConformidadMapper mapper;

    public Page<NoConformidadDTO> listar(SeveridadNoConformidad severidad,
                                         OrigenNoConformidad origen,
                                         Pageable pageable) {
        Page<NoConformidad> page;
        if (severidad != null && origen != null) {
            page = repository.findBySeveridadAndOrigen(severidad, origen, pageable);
        } else if (severidad != null) {
            page = repository.findBySeveridad(severidad, pageable);
        } else if (origen != null) {
            page = repository.findByOrigen(origen, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(mapper::toDTO);
    }

    @Transactional
    public NoConformidadDTO crear(NoConformidadDTO dto) {
        if (repository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una no conformidad con código: " + dto.getCodigo());
        }
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioReportaId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioReportaId()));
        NoConformidad entity = mapper.toEntity(dto, usuario);
        if (entity.getEstado() == null) {
            entity.setEstado(EstadoNoConformidad.ABIERTA);
        }
        if (entity.getCreadoPor() == null && usuario.getId() != null) {
            entity.setCreadoPor(usuario.getId());
        }
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public NoConformidadDTO actualizar(Long id, NoConformidadDTO dto) {
        NoConformidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + id));
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioReportaId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioReportaId()));
        existing.setCodigo(dto.getCodigo());
        existing.setOrigen(dto.getOrigen());
        existing.setSeveridad(dto.getSeveridad());
        if (dto.getEstado() != null) {
            existing.setEstado(dto.getEstado());
        }
        existing.setDescripcion(dto.getDescripcion());
        existing.setEvidencia(dto.getEvidencia());
        existing.setFechaRegistro(dto.getFechaRegistro());
        existing.setUsuarioReporta(usuario);
        existing.setActualizadoPor(usuario.getId());
        existing.setActualizadoEn(LocalDateTime.now());
        return mapper.toDTO(repository.save(existing));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public NoConformidadDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("No conformidad no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public NoConformidad registrarDesdeEvaluacion(LoteProducto lote,
                                                  EvaluacionCalidad evaluacion,
                                                  SeveridadNoConformidad severidad,
                                                  String descripcion,
                                                  Usuario usuario) {
        if (lote == null || lote.getId() == null) {
            throw new IllegalArgumentException("Lote obligatorio para registrar no conformidad");
        }
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("Usuario autenticado requerido");
        }

        Long loteId = lote.getId();
        Long evaluacionId = evaluacion != null ? evaluacion.getId() : null;

        Optional<NoConformidad> existente = Optional.empty();
        if (evaluacionId != null) {
            existente = repository.findFirstByLote_IdAndEvaluacion_IdAndEstado(loteId, evaluacionId, EstadoNoConformidad.ABIERTA);
        }
        if (existente.isEmpty()) {
            existente = repository.findFirstByLote_IdAndEstadoOrderByFechaRegistroDesc(loteId, EstadoNoConformidad.ABIERTA);
        }

        if (existente.isPresent()) {
            NoConformidad nc = existente.get();
            if (esSeveridadMasCritica(severidad, nc.getSeveridad())) {
                nc.setSeveridad(severidad);
            }
            if (descripcion != null && !descripcion.isBlank()) {
                nc.setDescripcion(descripcion);
            }
            if (evaluacion != null && nc.getEvaluacion() == null) {
                nc.setEvaluacion(evaluacion);
            }
            nc.setActualizadoPor(usuario.getId());
            nc.setActualizadoEn(LocalDateTime.now());
            return repository.save(nc);
        }

        NoConformidad nueva = new NoConformidad();
        nueva.setCodigo(generarCodigo());
        nueva.setOrigen(OrigenNoConformidad.LOTE);
        nueva.setSeveridad(severidad);
        nueva.setEstado(EstadoNoConformidad.ABIERTA);
        nueva.setDescripcion(descripcion);
        nueva.setFechaRegistro(LocalDateTime.now());
        nueva.setUsuarioReporta(usuario);
        nueva.setLote(lote);
        nueva.setProducto(lote.getProducto());
        nueva.setEvaluacion(evaluacion);
        nueva.setCreadoPor(usuario.getId());

        return repository.save(nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NoConformidad> obtenerActivaPorLote(Long loteId) {
        if (loteId == null) {
            return Optional.empty();
        }
        return repository.findFirstByLote_IdAndEstadoOrderByFechaRegistroDesc(loteId, EstadoNoConformidad.ABIERTA);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NoConformidad> obtenerActivaPorLoteYEvaluacion(Long loteId, Long evaluacionId) {
        if (loteId == null || evaluacionId == null) {
            return Optional.empty();
        }
        return repository.findFirstByLote_IdAndEvaluacion_IdAndEstado(loteId, evaluacionId, EstadoNoConformidad.ABIERTA);
    }

    private boolean esSeveridadMasCritica(SeveridadNoConformidad nueva, SeveridadNoConformidad actual) {
        if (nueva == null || actual == null) {
            return false;
        }
        return obtenerPesoSeveridad(nueva) < obtenerPesoSeveridad(actual);
    }

    private int obtenerPesoSeveridad(SeveridadNoConformidad severidad) {
        return switch (severidad) {
            case CRITICA -> 0;
            case MAYOR -> 1;
            case MENOR -> 2;
        };
    }

    private String generarCodigo() {
        String codigo;
        do {
            codigo = "NC-" + System.currentTimeMillis();
        } while (repository.existsByCodigo(codigo));
        return codigo;
    }
}

