package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.dto.NoConformidadDetalleDTO;
import com.willyes.clemenintegra.calidad.mapper.NoConformidadMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.OrigenNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoIncidente;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCapa;
import com.willyes.clemenintegra.calidad.repository.*;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoConformidadServiceImpl implements NoConformidadService {

    private static final DateTimeFormatter CODIGO_PERIODO_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DESCRIPCION_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NoConformidadRepository repository;
    private final RetencionLoteRepository retencionLoteRepository;
    private final RetencionLoteService retencionLoteService;
    private final UsuarioRepository usuarioRepository;
    private final CapaRepository capaRepository;
    private final NoConformidadMapper mapper;

    public Page<NoConformidadDTO> listar(SeveridadNoConformidad severidad,
                                         OrigenNoConformidad origen,
                                         TipoIncidente tipoIncidente,
                                         Pageable pageable) {
        Page<NoConformidadListadoProjection> page = repository.findListado(severidad, origen, tipoIncidente, pageable);
        return page.map(this::toListadoDTO);
    }

    @Override
    @Transactional
    public NoConformidadDTO crear(NoConformidadDTO dto, Usuario authUser) {
        if (dto == null) {
            throw new IllegalArgumentException("La no conformidad es obligatoria");
        }

        Usuario usuarioReporta = usuarioRepository.findById(dto.getUsuarioReportaId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Usuario no encontrado con ID: " + dto.getUsuarioReportaId()));
        Usuario usuarioActual = authUser != null ? authUser : usuarioReporta;
        boolean solicitarRetencion = Boolean.TRUE.equals(dto.getRetener());

        Optional<NoConformidad> existente = buscarNoConformidadActiva(dto.getLoteId(), dto.getEvaluacionId());
        NoConformidad resultado;
        if (existente.isPresent()) {
            NoConformidad nc = existente.get();
            EvaluacionCalidad evaluacion = dto.getEvaluacionId() != null
                    ? EvaluacionCalidad.builder().id(dto.getEvaluacionId()).build()
                    : null;
            actualizarNoConformidadExistente(nc, dto.getSeveridad(), dto.getDescripcion(), evaluacion, usuarioActual);
            resultado = repository.save(nc);
        } else {
            NoConformidad entity = mapper.toEntity(dto, usuarioReporta);
            if (entity.getCodigo() == null || entity.getCodigo().isBlank()) {
                entity.setCodigo(generarCodigoSecuencial());
            } else if (repository.existsByCodigo(entity.getCodigo())) {
                throw new IllegalArgumentException("Ya existe una no conformidad con código: " + entity.getCodigo());
            }
            if (entity.getEstado() == null) {
                entity.setEstado(EstadoNoConformidad.ABIERTA);
            }
            if (entity.getTipoIncidente() == null) {
                entity.setTipoIncidente(com.willyes.clemenintegra.calidad.model.enums.TipoIncidente.NO_CONFORMIDAD);
            }
            if (entity.getFechaRegistro() == null) {
                entity.setFechaRegistro(LocalDateTime.now());
            }
            Long usuarioId = usuarioActual != null ? usuarioActual.getId() : usuarioReporta.getId();
            if (entity.getCreadoPor() == null && usuarioId != null) {
                entity.setCreadoPor(usuarioId);
            }
            if (usuarioId != null) {
                entity.setActualizadoPor(usuarioId);
            }
            entity.setActualizadoEn(LocalDateTime.now());
            resultado = repository.save(entity);
        }

        if (solicitarRetencion
                && resultado.getLote() != null
                && resultado.getLote().getId() != null) {
            Usuario aprobador = usuarioActual != null ? usuarioActual : usuarioReporta;
            retencionLoteService.retener(
                    resultado.getLote().getId(),
                    MotivoRetencion.NO_CONFORMIDAD,
                    "NC " + resultado.getCodigo(),
                    resultado,
                    aprobador);
        }

        vincularRetencionConNoConformidad(resultado);
        return mapper.toDTO(resultado);
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
        if (dto.getTipoIncidente() != null) {
            existing.setTipoIncidente(dto.getTipoIncidente());
        }
        existing.setDescripcion(dto.getDescripcion());
        existing.setEvidencia(dto.getEvidencia());
        existing.setFechaRegistro(dto.getFechaRegistro());
        if (dto.getFechaCierre() != null) {
            existing.setFechaCierre(dto.getFechaCierre());
        }
        existing.setUsuarioReporta(usuario);
        existing.setActualizadoPor(usuario.getId());
        existing.setActualizadoEn(LocalDateTime.now());
        return mapper.toDTO(repository.save(existing));
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public NoConformidadDetalleDTO obtenerPorId(Long id) {

        NoConformidadDetalleProjection p = repository.findDetalleProjectionById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No conformidad no encontrada con ID: " + id
                ));

        return new NoConformidadDetalleDTO(
                p.getId(),
                p.getCodigo(),
                p.getOrigen(),
                p.getSeveridad(),
                p.getTipoIncidente(),
                p.getEstado(),
                p.getDescripcion(),
                p.getEvidencia(),
                p.getFechaRegistro(),
                p.getFechaCierre(),
                p.getUsuarioReportaId(),
                p.getLoteId(),
                p.getProductoId(),
                p.getEvaluacionId(),
                p.getCodigoLote(),
                p.getReportadoPorNombre(),
                p.getProductoNombre(),
                p.getCreadoPor(),
                p.getActualizadoPor(),
                p.getActualizadoEn()
        );
    }


    @Override
    @Transactional
    public NoConformidadDTO cerrar(Long id, Usuario authUser) {
        NoConformidad noConformidad = repository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(
                        ApiErrorCode.NC_NO_ENCONTRADA,
                        "No conformidad no encontrada.",
                        Map.of("ncId", id)));

        if (noConformidad.getEstado() == EstadoNoConformidad.CERRADA) {
            throw new CustomBusinessException(
                    ApiErrorCode.NC_YA_CERRADA,
                    "La no conformidad ya se encuentra cerrada.",
                    Map.of("ncId", id));
        }

        boolean tieneCapaCerrada = capaRepository.existsByNoConformidad_IdAndEstado(
                id, EstadoCapa.CERRADA);
        if (!tieneCapaCerrada) {
            throw new CustomBusinessException(
                    ApiErrorCode.NC_CAPA_REQUERIDA,
                    "Debe existir al menos una CAPA cerrada para cerrar la no conformidad.",
                    Map.of("ncId", id));
        }

        noConformidad.setEstado(EstadoNoConformidad.CERRADA);
        noConformidad.setFechaCierre(LocalDateTime.now());
        Long usuarioId = authUser != null ? authUser.getId() : noConformidad.getActualizadoPor();
        if (usuarioId != null) {
            noConformidad.setActualizadoPor(usuarioId);
        }
        noConformidad.setActualizadoEn(LocalDateTime.now());

        NoConformidad guardada = repository.save(noConformidad);
        return mapper.toDTO(guardada);
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

        Optional<NoConformidad> existente = buscarNoConformidadActiva(loteId, evaluacionId);
        if (existente.isPresent()) {
            NoConformidad nc = existente.get();
            actualizarNoConformidadExistente(nc, severidad, descripcion, evaluacion, usuario);
            NoConformidad guardada = repository.save(nc);
            vincularRetencionConNoConformidad(guardada);
            return guardada;
        }

        NoConformidad nueva = new NoConformidad();
        nueva.setCodigo(generarCodigoSecuencial());
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
        nueva.setActualizadoPor(usuario.getId());
        nueva.setActualizadoEn(LocalDateTime.now());

        NoConformidad guardada = repository.save(nueva);
        vincularRetencionConNoConformidad(guardada);
        return guardada;
    }

    private NoConformidadDTO toListadoDTO(NoConformidadListadoProjection projection) {
        return NoConformidadDTO.builder()
                .id(projection.getId())
                .codigo(projection.getCodigo())
                .origen(projection.getOrigen())
                .severidad(projection.getSeveridad())
                .estado(projection.getEstado())
                .tipoIncidente(projection.getTipoIncidente())
                .descripcion(projection.getDescripcion())
                .evidencia(projection.getEvidencia())
                .fechaRegistro(projection.getFechaRegistro())
                .fechaCierre(projection.getFechaCierre())
                .usuarioReportaId(projection.getUsuarioReportaId())
                .loteId(projection.getLoteId())
                .productoId(projection.getProductoId() != null ? projection.getProductoId().longValue() : null)
                .evaluacionId(projection.getEvaluacionId())
                .codigoLote(projection.getCodigoLote())
                .reportadoPorNombre(projection.getReportadoPorNombre())
                .productoNombre(projection.getProductoNombre())
                .creadoPor(projection.getCreadoPor())
                .actualizadoPor(projection.getActualizadoPor())
                .actualizadoEn(projection.getActualizadoEn())
                .build();
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

    private Optional<NoConformidad> buscarNoConformidadActiva(Long loteId, Long evaluacionId) {
        if (loteId == null) {
            return Optional.empty();
        }
        if (evaluacionId != null) {
            Optional<NoConformidad> porEvaluacion = repository.findFirstByLote_IdAndEvaluacion_IdAndEstado(
                    loteId, evaluacionId, EstadoNoConformidad.ABIERTA);
            if (porEvaluacion.isPresent()) {
                return porEvaluacion;
            }
        }
        return repository.findFirstByLote_IdAndEstadoOrderByFechaRegistroDesc(loteId, EstadoNoConformidad.ABIERTA);
    }

    private void actualizarNoConformidadExistente(NoConformidad existente,
                                                  SeveridadNoConformidad nuevaSeveridad,
                                                  String descripcionNueva,
                                                  EvaluacionCalidad evaluacion,
                                                  Usuario usuarioActual) {
        boolean modificado = false;
        if (esSeveridadMasCritica(nuevaSeveridad, existente.getSeveridad())) {
            existente.setSeveridad(nuevaSeveridad);
            modificado = true;
        }
        if (descripcionNueva != null && !descripcionNueva.isBlank()) {
            existente.setDescripcion(agregarDescripcionConMarcaTiempo(existente.getDescripcion(), descripcionNueva));
            modificado = true;
        }
        if (evaluacion != null && existente.getEvaluacion() == null) {
            existente.setEvaluacion(evaluacion);
            modificado = true;
        }
        if (modificado || usuarioActual != null) {
            Long usuarioId = usuarioActual != null ? usuarioActual.getId() : existente.getActualizadoPor();
            existente.setActualizadoPor(usuarioId);
            existente.setActualizadoEn(LocalDateTime.now());
        }
    }

    private String agregarDescripcionConMarcaTiempo(String descripcionActual, String nuevaDescripcion) {
        if (nuevaDescripcion == null || nuevaDescripcion.isBlank()) {
            return descripcionActual;
        }
        String entrada = "[" + LocalDateTime.now().format(DESCRIPCION_TIMESTAMP_FORMAT) + "] "
                + nuevaDescripcion.trim();
        if (descripcionActual == null || descripcionActual.isBlank()) {
            return entrada;
        }
        return descripcionActual + System.lineSeparator() + entrada;
    }

    private void vincularRetencionConNoConformidad(NoConformidad noConformidad) {
        if (noConformidad == null || noConformidad.getLote() == null || noConformidad.getLote().getId() == null) {
            return;
        }
        Long loteId = noConformidad.getLote().getId();
        retencionLoteRepository.findFirstByLote_IdAndEstadoAndMotivo(loteId, EstadoRetencion.RETENIDO,
                        MotivoRetencion.NO_CONFORMIDAD)
                .filter(retencion -> retencion.getNoConformidad() == null)
                .ifPresent(retencion -> {
                    retencion.setNoConformidad(noConformidad);
                    retencionLoteRepository.save(retencion);
                });
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

    private String generarCodigoSecuencial() {
        YearMonth periodo = YearMonth.now();
        String prefijo = "NC-" + periodo.format(CODIGO_PERIODO_FORMAT);
        int correlativo = repository.findFirstByCodigoStartingWithOrderByCodigoDesc(prefijo)
                .map(NoConformidad::getCodigo)
                .map(this::extraerCorrelativo)
                .orElse(0) + 1;

        String codigo;
        do {
            codigo = String.format("%s-%03d", prefijo, correlativo);
            correlativo++;
        } while (repository.existsByCodigo(codigo));
        return codigo;
    }

    private int extraerCorrelativo(String codigo) {
        if (codigo == null || !codigo.contains("-")) {
            return 0;
        }
        String secuencia = codigo.substring(codigo.lastIndexOf('-') + 1);
        try {
            return Integer.parseInt(secuencia);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
