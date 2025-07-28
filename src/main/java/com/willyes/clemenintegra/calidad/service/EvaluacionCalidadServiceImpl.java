package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EvaluacionCalidadServiceImpl implements EvaluacionCalidadService {

    private final EvaluacionCalidadRepository repository;
    private final LoteProductoRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final EvaluacionCalidadMapper mapper;

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
        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Usuario user = usuarioService.obtenerUsuarioAutenticado();

        // Validar rol de acuerdo al tipo de evaluación
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

        // Evitar duplicado de lote + tipo
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

        // Construye y persiste la entidad
        EvaluacionCalidad entidad = mapper.toEntity(dto, lote, user);
        entidad.setFechaEvaluacion(LocalDateTime.now());
        entidad.setArchivosAdjuntos(adjuntos);

        entidad = repository.save(entidad);

        return mapper.toResponseDTO(entidad);
    }

    public EvaluacionCalidadResponseDTO actualizar(Long id, EvaluacionCalidadRequestDTO dto) {
        EvaluacionCalidad existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));

        LoteProducto lote = loteRepository.findById(dto.getLoteProductoId())
                .orElseThrow(() -> new NoSuchElementException("Lote no encontrado con ID: " + dto.getLoteProductoId()));

        Usuario user = usuarioRepository.findById(dto.getUsuarioEvaluadorId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + dto.getUsuarioEvaluadorId()));

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
                .map(entry -> {
                    LoteProducto lote = entry.getKey();
                    return EvaluacionConsolidadaResponseDTO.builder()
                            .nombreLote(lote.getCodigoLote())
                            .nombreProducto(lote.getProducto().getNombre())
                            .estadoLote(lote.getEstado().name())
                            .tipoAnalisisCalidad(lote.getProducto().getTipoAnalisisCalidad().name())
                            .evaluaciones(entry.getValue().stream()
                                    .map(mapper::toSimpleDTO)
                                    .toList())
                            .build();
                })
                .toList();
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

}
