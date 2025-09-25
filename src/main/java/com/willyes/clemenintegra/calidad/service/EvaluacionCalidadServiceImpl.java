package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluacionCalidadServiceImpl implements EvaluacionCalidadService {

    private final EvaluacionCalidadRepository repository;
    private final LoteProductoRepository loteRepository;
    private final EvaluacionCalidadMapper mapper;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final InventoryCatalogResolver catalogResolver;
    private final AlmacenRepository almacenRepository;

    public Page<EvaluacionCalidadResponseDTO> listar(ResultadoEvaluacion resultado, Pageable pageable) {
        Page<EvaluacionCalidad> page = (resultado != null)
                ? repository.findByResultado(resultado, pageable)
                : repository.findAll(pageable);
        return page.map(mapper::toResponseDTO);
    }

    public Page<EvaluacionCalidadResponseDTO> listarPorFecha(LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);
        Page<EvaluacionCalidad> page = repository.findAllByFechaEvaluacionBetween(inicio, fin, pageable);
        return page.map(mapper::toResponseDTO);
    }

    public EvaluacionCalidadResponseDTO crear(EvaluacionCalidadRequestDTO dto, java.util.List<MultipartFile> archivos) {
        Usuario user = usuarioService.obtenerUsuarioAutenticado();

        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        String operacion = buildOperacion("registrarEvaluacion", dto.getTipoEvaluacion());
        auditarYRestaurarCuarentena(lote, cuarentenaId, operacion, user);

        if (dto.getTipoEvaluacion() == TipoEvaluacion.FISICO_QUIMICO
                && !java.util.Set.of(RolUsuario.ROL_ANALISTA_CALIDAD, RolUsuario.ROL_JEFE_CALIDAD).contains(user.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un analista o el jefe de calidad puede registrar evaluaciones físico-químicas");
        }
        if (dto.getTipoEvaluacion() == TipoEvaluacion.MICROBIOLOGICO
                && !java.util.Set.of(RolUsuario.ROL_MICROBIOLOGO, RolUsuario.ROL_JEFE_CALIDAD).contains(user.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un microbiólogo o el jefe de calidad puede registrar evaluaciones microbiológicas");
        }

        if (repository.existsByLoteProductoIdAndTipoEvaluacion(lote.getId(), dto.getTipoEvaluacion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una evaluación de este tipo para el lote");
        }

        if (archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe adjuntar al menos un documento.");
        }

        java.util.List<ArchivoEvaluacion> adjuntos = new java.util.ArrayList<>();
        java.util.List<ArchivoEvaluacionDTO> datosArchivos = dto.getArchivosAdjuntos();

        for (int i = 0; i < archivos.size(); i++) {
            MultipartFile archivo = archivos.get(i);
            if (archivo == null || archivo.isEmpty()) continue;
            try {
                String nombreOriginal = archivo.getOriginalFilename();
                String nombreSanitizado = (nombreOriginal != null ? nombreOriginal : "archivo")
                        .replaceAll("[^a-zA-Z0-9._-]", "_");

                String nombreArchivo = System.currentTimeMillis() + "_" + nombreSanitizado;

                Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones");
                Files.createDirectories(uploadRoot);

                Path destino = uploadRoot.resolve(nombreArchivo);
                archivo.transferTo(destino.toFile());

                String nombreVisible = (datosArchivos != null && datosArchivos.size() > i)
                        ? datosArchivos.get(i).getNombreVisible()
                        : nombreOriginal;

                adjuntos.add(ArchivoEvaluacion.builder()
                        .nombreArchivo(nombreArchivo)
                        .nombreVisible(nombreVisible)
                        .build());
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar el archivo adjunto: " + e.getMessage(), e);
            }
        }

        EvaluacionCalidad entidad = mapper.toEntity(dto, lote, user);
        entidad.setFechaEvaluacion(LocalDateTime.now());
        entidad.setArchivosAdjuntos(adjuntos);

        entidad = repository.save(entidad);

        verificarAlmacenPostOperacion(lote.getId(), cuarentenaId, operacion, user);

        return mapper.toResponseDTO(entidad);
    }

    public EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto) {
        EvaluacionCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));

        Usuario user = usuarioRepository.findById(dto.getUsuarioEvaluadorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioEvaluadorId()));

        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        String operacion = buildOperacion("actualizarEvaluacion", dto.getTipoEvaluacion());
        auditarYRestaurarCuarentena(lote, cuarentenaId, operacion, user);

        existing.setResultado(dto.getResultado());
        existing.setTipoEvaluacion(dto.getTipoEvaluacion());
        existing.setObservaciones(dto.getObservaciones());

        java.util.List<ArchivoEvaluacion> nuevosAdjuntos = (dto.getArchivosAdjuntos() == null)
                ? new java.util.ArrayList<>()
                : dto.getArchivosAdjuntos().stream()
                .map(a -> ArchivoEvaluacion.builder()
                        .nombreArchivo(a.getNombreArchivo())
                        .nombreVisible(a.getNombreVisible())
                        .build())
                .toList();
        existing.setArchivosAdjuntos(nuevosAdjuntos);
        existing.setLoteProducto(lote);
        existing.setUsuarioEvaluador(user);
        existing.setFechaEvaluacion(LocalDateTime.now());

        existing = repository.save(existing);

        verificarAlmacenPostOperacion(lote.getId(), cuarentenaId, operacion, user);

        return mapper.toResponseDTO(existing);
    }

    public EvaluacionCalidadResponseDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));
    }

    @Override
    public java.util.List<EvaluacionCalidadResponseDTO> listarPorLote(Long loteId) {
        return repository.findByLoteProductoId(loteId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Override
    public java.util.List<EvaluacionConsolidadaResponseDTO> obtenerEvaluacionesConsolidadas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        java.util.List<EvaluacionCalidad> evaluaciones = repository.findAllWithinFechaEvaluacion(inicio, fin);

        java.util.Map<LoteProducto, java.util.List<EvaluacionCalidad>> agrupado = evaluaciones.stream()
                .collect(java.util.stream.Collectors.groupingBy(EvaluacionCalidad::getLoteProducto));

        return agrupado.entrySet().stream()
                .map(entry -> mapper.toConsolidadoDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private String buildOperacion(String prefijo, TipoEvaluacion tipo) {
        return prefijo + (tipo != null ? tipo.name() : "");
    }

    private void auditarYRestaurarCuarentena(LoteProducto lote, Long cuarentenaId,
                                             String operacion, Usuario usuarioActual) {
        if (lote == null) {
            return;
        }
        if (cuarentenaId == null) {
            log.error("AUDIT_CALIDAD: id de almacén de cuarentena no configurado. loteId={} operacion={} usuario={} rol={}",
                    lote.getId(), operacion,
                    usuarioActual != null ? usuarioActual.getId() : null,
                    usuarioActual != null ? usuarioActual.getRol() : null);
            return;
        }
        Long almacenActual = obtenerAlmacenId(lote);
        if (!Objects.equals(almacenActual, cuarentenaId)) {
            log.warn("AUDIT_CALIDAD: cambiando almacen loteId={} de {} a {} por {}. Usuario={} rol={} estado={} operacion={}",
                    lote.getId(), almacenActual, cuarentenaId, "pre-evaluacion",
                    usuarioActual != null ? usuarioActual.getId() : null,
                    usuarioActual != null ? usuarioActual.getRol() : null,
                    lote.getEstado(), operacion);
            restaurarCuarentena(lote, cuarentenaId, operacion, usuarioActual, almacenActual);
        }
    }

    private void verificarAlmacenPostOperacion(Long loteId, Long cuarentenaId,
                                               String operacion, Usuario usuarioActual) {
        if (loteId == null || cuarentenaId == null) {
            return;
        }
        loteRepository.findById(loteId).ifPresent(actual -> {
            Long almacenActual = obtenerAlmacenId(actual);
            if (!Objects.equals(almacenActual, cuarentenaId)) {
                log.warn("AUDIT_CALIDAD: detectado cambio de almacén tras {}. loteId={} almacenId={} estado={}",
                        operacion, loteId, almacenActual, actual.getEstado());
                restaurarCuarentena(actual, cuarentenaId, operacion, usuarioActual, almacenActual);
            }
        });
    }

    private void restaurarCuarentena(LoteProducto lote, Long cuarentenaId,
                                     String operacion, Usuario usuarioActual, Long almacenPrevio) {
        if (lote == null || cuarentenaId == null) {
            return;
        }
        Almacen cuarentena = almacenRepository.findById(cuarentenaId)
                .orElseGet(() -> new Almacen(Math.toIntExact(cuarentenaId)));
        lote.setAlmacen(cuarentena);
        loteRepository.saveAndFlush(lote);
        log.warn("AUDIT_CALIDAD: cambiando almacen loteId={} de {} a {} por {}. Usuario={} rol={} estado={} operacion={}",
                lote.getId(), almacenPrevio, cuarentenaId, "restaurar-cuarentena",
                usuarioActual != null ? usuarioActual.getId() : null,
                usuarioActual != null ? usuarioActual.getRol() : null,
                lote.getEstado(), operacion);
    }

    private Long obtenerAlmacenId(LoteProducto lote) {
        if (lote == null || lote.getAlmacen() == null || lote.getAlmacen().getId() == null) {
            return null;
        }
        return lote.getAlmacen().getId().longValue();
    }
}
