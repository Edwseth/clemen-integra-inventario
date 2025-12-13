package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoCreateDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCondicionDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
import com.willyes.clemenintegra.calidad.mapper.EvaluacionCalidadMapper;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.calidad.model.enums.SeveridadNoConformidad;
import com.willyes.clemenintegra.calidad.service.CondicionUsoService;
import com.willyes.clemenintegra.calidad.service.RetencionLoteService;
import com.willyes.clemenintegra.calidad.service.NoConformidadService;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.model.enums.RolUsuario;
import com.willyes.clemenintegra.shared.repository.UsuarioRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
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
    private final CondicionUsoService condicionUsoService;
    private final RetencionLoteService retencionLoteService;
    private final NoConformidadService noConformidadService;
    private final ResultadoAnalisisMicroService resultadoAnalisisMicroService;
    private final ResultadoAnalisisMicrobiologicoRepository resultadoAnalisisMicrobiologicoRepository;

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

        // FAIL-FAST: solo se permite registrar evaluación si el lote sigue en cuarentena/retención
        if (!estaEnCuarentenaOLotenRetenido(lote)) {
            log.info("[CALIDAD] intento de registrar evaluación con lote fuera de cuarentena/retención. loteId={} estado={} almacenId={} usuario={}",
                    lote.getId(), (lote.getEstado() != null ? lote.getEstado().name() : null), obtenerAlmacenId(lote),
                    user != null ? user.getId() : null);
            throw new CustomBusinessException(
                    ApiErrorCode.BLOQUEO_ESTADO_CUARENTENA,
                    "El lote debe permanecer en CUARENTENA o RETENIDO para registrar evaluaciones. Reabra el lote desde calidad antes de continuar.",
                    Map.of(
                            "loteId", lote.getId(),
                            "estadoActual", lote.getEstado() != null ? lote.getEstado().name() : null));
        }

        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        String operacion = buildOperacion("registrarEvaluacion", dto.getTipoEvaluacion());
        auditarYRestaurarCuarentena(lote, cuarentenaId, operacion, user);

        validarRolEvaluador(user, dto.getTipoEvaluacion());

        java.util.List<ArchivoEvaluacion> adjuntos = guardarAdjuntos(archivos, dto.getArchivosAdjuntos());

        EvaluacionCalidad entidad;
        if (dto.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO) {
            entidad = repository.findFirstByLoteProductoIdAndTipoEvaluacion(lote.getId(), TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                    .map(existing -> {
                        existing.setResultado(dto.getResultado());
                        existing.setObservaciones(dto.getObservaciones());
                        existing.setFechaEvaluacion(LocalDateTime.now());
                        existing.setUsuarioEvaluador(user);
                        existing.setTipoEvaluacion(TipoEvaluacion.QUIMICO_MICROBIOLOGICO);
                        existing.setLoteProducto(lote);

                        if (adjuntos != null && !adjuntos.isEmpty()) {
                            java.util.List<ArchivoEvaluacion> actuales = existing.getArchivosAdjuntos();
                            if (actuales == null) {
                                actuales = new java.util.ArrayList<>();
                            }
                            actuales.addAll(adjuntos);
                            existing.setArchivosAdjuntos(actuales);
                        }
                        return existing;
                    })
                    .orElseGet(() -> {
                        EvaluacionCalidad nueva = mapper.toEntity(dto, lote, user);
                        nueva.setFechaEvaluacion(LocalDateTime.now());
                        nueva.setArchivosAdjuntos(adjuntos);
                        return nueva;
                    });
        } else {
            entidad = mapper.toEntity(dto, lote, user);
            entidad.setFechaEvaluacion(LocalDateTime.now());
            entidad.setArchivosAdjuntos(adjuntos);
        }

        entidad = repository.save(entidad);

        validarRolEvaluador(entidad.getUsuarioEvaluador(), entidad.getTipoEvaluacion());
        manejarResultadoEvaluacion(entidad, dto, lote, user);

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

        // FAIL-FAST: no permitir actualizar evaluaciones si el lote ya no está en estado de cuarentena/retención
        if (!estaEnCuarentenaOLotenRetenido(lote)) {
            log.info("[CALIDAD] intento de actualizar evaluación con lote fuera de cuarentena/retención. loteId={} estado={} almacenId={} usuario={}",
                    lote.getId(), (lote.getEstado() != null ? lote.getEstado().name() : null), obtenerAlmacenId(lote),
                    user != null ? user.getId() : null);
            throw new CustomBusinessException(
                    ApiErrorCode.BLOQUEO_ESTADO_CUARENTENA,
                    "El lote debe permanecer en CUARENTENA o RETENIDO para actualizar evaluaciones. Reabra el lote desde calidad antes de continuar.",
                    Map.of(
                            "loteId", lote.getId(),
                            "estadoActual", lote.getEstado() != null ? lote.getEstado().name() : null));
        }

        Long cuarentenaId = catalogResolver.getAlmacenCuarentenaId();
        String operacion = buildOperacion("actualizarEvaluacion", dto.getTipoEvaluacion());
        auditarYRestaurarCuarentena(lote, cuarentenaId, operacion, user);

        validarRolEvaluador(user, dto.getTipoEvaluacion());

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

        Usuario actor = usuarioService.obtenerUsuarioAutenticado();
        if (actor == null) {
            actor = user;
        }
        validarRolEvaluador(existing.getUsuarioEvaluador(), existing.getTipoEvaluacion());
        manejarResultadoEvaluacion(existing, dto, lote, actor);

        verificarAlmacenPostOperacion(lote.getId(), cuarentenaId, operacion, user);

        return mapper.toResponseDTO(existing);
    }

    public EvaluacionCalidadResponseDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + id));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public EvaluacionCalidadDetalleDTO obtenerDetalle(Long id) {
        EvaluacionCalidad evaluacion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluación no encontrada"));
        java.util.List<ResultadoAnalisisMicroResponseDTO> resultados = resultadoAnalisisMicroService.obtenerPorEvaluacion(id);
        return mapper.toDetalleDTO(evaluacion, evaluacion.getLoteProducto().getProducto(),
                mapper.mapearResultadosMicro(resultados));
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

        java.util.Set<Long> evaluacionesQuimicoMicro = evaluaciones.stream()
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .map(EvaluacionCalidad::getId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> evaluacionesConResultadosMicro = evaluacionesQuimicoMicro.isEmpty()
                ? java.util.Collections.emptySet()
                : resultadoAnalisisMicrobiologicoRepository.findByEvaluacionIdIn(evaluacionesQuimicoMicro.stream().toList())
                .stream()
                .map(r -> r.getEvaluacion().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, Boolean> conformidadMicro = resultadoAnalisisMicrobiologicoRepository
                .findByEvaluacionIdIn(evaluacionesQuimicoMicro.stream().toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getEvaluacion().getId(),
                        java.util.stream.Collectors.mapping(ResultadoAnalisisMicrobiologico::getCumple,
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toList(),
                                        lista -> lista.isEmpty() ? null : lista.stream().allMatch(Boolean.TRUE::equals)))));

        return agrupado.entrySet().stream()
                .map(entry -> mapper.toConsolidadoDTO(entry.getKey(), entry.getValue(),
                        evaluacionesConResultadosMicro, conformidadMicro))
                .toList();
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private java.util.List<ArchivoEvaluacion> guardarAdjuntos(java.util.List<MultipartFile> archivos,
                                                              java.util.List<ArchivoEvaluacionDTO> datosArchivos) {
        java.util.List<ArchivoEvaluacion> adjuntos = new java.util.ArrayList<>();
        for (int i = 0; archivos != null && i < archivos.size(); i++) {
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
        return adjuntos;
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
            log.error("[CALIDAD] id de almacén de cuarentena no configurado. loteId={} operacion={} usuario={} rol={}",
                    lote.getId(), operacion,
                    usuarioActual != null ? usuarioActual.getId() : null,
                    usuarioActual != null ? usuarioActual.getRol() : null);
            return;
        }
        Long almacenActual = obtenerAlmacenId(lote);
        // Solo restaurar si el lote SIGUE en estado de cuarentena/retención
        if (!Objects.equals(almacenActual, cuarentenaId) && estaEnCuarentenaOLotenRetenido(lote)) {
            log.info("[CALIDAD] restableciendo almacén de cuarentena. loteId={} almacenActual={} almacenDestino={} usuario={} rol={} estado={} operacion={}",
                    lote.getId(), almacenActual, cuarentenaId,
                    usuarioActual != null ? usuarioActual.getId() : null,
                    usuarioActual != null ? usuarioActual.getRol() : null,
                    lote.getEstado(), operacion);
            restaurarCuarentena(lote, cuarentenaId, operacion, usuarioActual, almacenActual);
        } else if (!estaEnCuarentenaOLotenRetenido(lote)) {
            // Si el estado ya no es de cuarentena/retención, no modificar almacén (posible liberación legítima)
            log.debug("[CALIDAD] lote fuera de estado de cuarentena/retención durante {}. loteId={} estado={} almacenId={}",
                    operacion, lote.getId(), (lote.getEstado() != null ? lote.getEstado().name() : null), almacenActual);
        }
    }

    private void verificarAlmacenPostOperacion(Long loteId, Long cuarentenaId,
                                               String operacion, Usuario usuarioActual) {
        if (loteId == null || cuarentenaId == null) {
            return;
        }
        loteRepository.findById(loteId).ifPresent(actual -> {
            Long almacenActual = obtenerAlmacenId(actual);
            if (!Objects.equals(almacenActual, cuarentenaId) && estaEnCuarentenaOLotenRetenido(actual)) {
                log.info("[CALIDAD] detectado cambio de almacén tras {}. loteId={} almacenId={} estado={}",
                        operacion, loteId, almacenActual, actual.getEstado());
                restaurarCuarentena(actual, cuarentenaId, operacion, usuarioActual, almacenActual);
            } else if (!estaEnCuarentenaOLotenRetenido(actual)) {
                log.debug("[CALIDAD] post-{}: lote fuera de estado de cuarentena/retención; no se restaura. loteId={} estado={} almacenId={}",
                        operacion, loteId, (actual.getEstado() != null ? actual.getEstado().name() : null), almacenActual);
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
        log.info("[CALIDAD] restaurando lote a cuarentena. loteId={} almacenPrevio={} almacenDestino={} usuario={} rol={} estado={} operacion={}",
                lote.getId(), almacenPrevio, cuarentenaId,
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

    private boolean estaEnCuarentenaOLotenRetenido(LoteProducto lote) {
        if (lote == null || lote.getEstado() == null) return false;
        String nombre = lote.getEstado().name();
        return "EN_CUARENTENA".equals(nombre) || "RETENIDO".equals(nombre);
    }

    private void validarRolEvaluador(Usuario evaluador, TipoEvaluacion tipoEvaluacion) {
        if (evaluador == null || tipoEvaluacion == null) {
            return;
        }
        RolUsuario rol = evaluador.getRol();
        if (rol == null) {
            throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE,
                    "El usuario evaluador no posee un rol asignado para esta evaluación.");
        }
        switch (tipoEvaluacion) {
            case FISICO -> {
                if (!java.util.Set.of(RolUsuario.ROL_ANALISTA_CALIDAD,
                        RolUsuario.ROL_JEFE_CALIDAD,
                        RolUsuario.ROL_SUPER_ADMIN).contains(rol)) {
                    throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE,
                            "Solo un analista, jefe de calidad o super admin puede registrar evaluaciones físicas.");
                }
            }
            case QUIMICO_MICROBIOLOGICO -> {
                if (!java.util.Set.of(RolUsuario.ROL_MICROBIOLOGO,
                        RolUsuario.ROL_JEFE_CALIDAD,
                        RolUsuario.ROL_SUPER_ADMIN).contains(rol)) {
                    throw new CustomBusinessException(ApiErrorCode.ROL_INSUFICIENTE,
                            "Solo un microbiólogo, jefe de calidad o super admin puede registrar evaluaciones químico-microbiológicas.");
                }
            }
            default -> {
            }
        }
    }

    @Override
    public byte[] generarReporteEvaluacionesExcel(LocalDate fechaInicio, LocalDate fechaFin, ResultadoEvaluacion resultado) {
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        java.util.List<EvaluacionCalidad> evaluaciones = repository.findAllWithRelations();

        java.util.stream.Stream<EvaluacionCalidad> stream = evaluaciones.stream();
        if (inicio != null) {
            stream = stream.filter(e -> e.getFechaEvaluacion() != null && !e.getFechaEvaluacion().isBefore(inicio));
        }
        if (fin != null) {
            stream = stream.filter(e -> e.getFechaEvaluacion() != null && !e.getFechaEvaluacion().isAfter(fin));
        }
        if (resultado != null) {
            stream = stream.filter(e -> e.getResultado() == resultado);
        }

        java.util.List<EvaluacionCalidad> filtradas = stream.toList();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Evaluaciones Calidad");

        String[] headers = {
                "Fecha evaluación", "Código Lote", "Producto", "Tipo análisis requerido", "Tipo de evaluación",
                "Resultado evaluación", "Resultado global del lote", "Estado lote", "Analista/Evaluador", "Tiene adjuntos", "Código NC asociada"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowIdx = 1;
        for (EvaluacionCalidad eval : filtradas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(eval.getFechaEvaluacion() != null ? eval.getFechaEvaluacion().toString() : "");
            row.createCell(1).setCellValue(eval.getLoteProducto() != null ? eval.getLoteProducto().getCodigoLote() : "");
            row.createCell(2).setCellValue(eval.getLoteProducto() != null && eval.getLoteProducto().getProducto() != null
                    ? eval.getLoteProducto().getProducto().getNombre() : "");
            row.createCell(3).setCellValue(eval.getLoteProducto() != null && eval.getLoteProducto().getProducto() != null
                    && eval.getLoteProducto().getProducto().getTipoAnalisisCalidad() != null
                    ? eval.getLoteProducto().getProducto().getTipoAnalisisCalidad().name() : "");
            row.createCell(4).setCellValue(eval.getTipoEvaluacion() != null ? eval.getTipoEvaluacion().name() : "");
            row.createCell(5).setCellValue(eval.getResultado() != null ? eval.getResultado().name() : "");
            row.createCell(6).setCellValue(eval.getResultado() != null ? eval.getResultado().name() : "");
            row.createCell(7).setCellValue(eval.getLoteProducto() != null && eval.getLoteProducto().getEstado() != null
                    ? eval.getLoteProducto().getEstado().name() : "");
            row.createCell(8).setCellValue(eval.getUsuarioEvaluador() != null ? eval.getUsuarioEvaluador().getNombreCompleto() : "");
            boolean tieneAdjuntos = eval.getArchivosAdjuntos() != null && !eval.getArchivosAdjuntos().isEmpty();
            row.createCell(9).setCellValue(tieneAdjuntos ? "SI" : "NO");
            String codigoNc = noConformidadService.obtenerActivaPorLoteYEvaluacion(
                    eval.getLoteProducto() != null ? eval.getLoteProducto().getId() : null,
                    eval.getId()).map(com.willyes.clemenintegra.calidad.model.NoConformidad::getCodigo).orElse("");
            row.createCell(10).setCellValue(codigoNc);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel de evaluaciones", e);
        }
    }

    private void manejarResultadoEvaluacion(EvaluacionCalidad evaluacion,
                                            EvaluacionCalidadRequestDTO dto,
                                            LoteProducto lote,
                                            Usuario usuario) {
        ResultadoEvaluacion resultado = dto.getResultado();
        if (resultado == null) {
            return;
        }
        if (resultado == ResultadoEvaluacion.CONDICIONADO) {
            EvaluacionCondicionDTO condicion = dto.getCondicion();
            if (condicion == null || condicion.getTipo() == null) {
                throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                        "Debe registrar la condición de uso para completar la evaluación condicionada.");
            }
            CondicionUsoCreateDTO createDTO = CondicionUsoCreateDTO.builder()
                    .loteId(lote.getId())
                    .tipo(condicion.getTipo())
                    .parametroFecha(condicion.getParametroFecha())
                    .descripcion(condicion.getDescripcion())
                    .build();
            condicionUsoService.create(createDTO, usuario);
        } else if (resultado == ResultadoEvaluacion.NO_CONFORME) {
            SeveridadNoConformidad severidad = dto.getSeveridadNc();
            if (severidad == null) {
                throw new CustomBusinessException(ApiErrorCode.EVALUACIONES_FALTANTES,
                        "Debe indicar la severidad de la no conformidad para continuar.");
            }
            var noConformidad = noConformidadService.registrarDesdeEvaluacion(lote, evaluacion, severidad,
                    dto.getObservaciones(), usuario);
            retencionLoteService.asegurarRetencionNoConformidad(lote, "NC pendiente", noConformidad, usuario);
        }
    }
}
