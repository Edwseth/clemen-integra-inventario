package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroRequestDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.ParametroAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.PlantillaAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.ResultadoAnalisisMicrobiologico;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.ResultadoAnalisisMicrobiologicoRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO;

@Service
@RequiredArgsConstructor
public class ResultadoAnalisisMicroServiceImpl implements ResultadoAnalisisMicroService {

    private final EvaluacionCalidadRepository evaluacionRepository;
    private final ResultadoAnalisisMicrobiologicoRepository resultadoRepository;
    private final AnalisisMicroPdfService analisisMicroPdfService;

    @Override
    @Transactional
    public List<ResultadoAnalisisMicroResponseDTO> guardarResultados(Long evaluacionId, List<ResultadoAnalisisMicroRequestDTO> payload) {
        EvaluacionCalidad evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + evaluacionId));

        PlantillaAnalisisMicrobiologico plantilla = Optional.ofNullable(evaluacion.getLoteProducto())
                .map(l -> l.getProducto().getPlantillaAnalisisMicrobiologico())
                .orElse(null);

        if (plantilla == null) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "El producto asociado no tiene una plantilla microbiológica configurada.");
        }

        if (evaluacion.getTipoEvaluacion() != TipoEvaluacion.QUIMICO_MICROBIOLOGICO) {
            throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                    "Solo se pueden registrar resultados microbiológicos sobre evaluaciones QM.");
        }

        Map<Long, ParametroAnalisisMicrobiologico> parametrosValidos = plantilla.getParametros().stream()
                .collect(java.util.stream.Collectors.toMap(ParametroAnalisisMicrobiologico::getId, p -> p));

        List<ResultadoAnalisisMicrobiologico> existentes = resultadoRepository.findByEvaluacionId(evaluacionId);
        if (!existentes.isEmpty()) {
            resultadoRepository.deleteAll(existentes);
        }

        List<ResultadoAnalisisMicrobiologico> aGuardar = new ArrayList<>();
        for (ResultadoAnalisisMicroRequestDTO dto : payload) {
            ParametroAnalisisMicrobiologico parametro = parametrosValidos.get(dto.getParametroId());
            if (parametro == null) {
                throw new CustomBusinessException(ApiErrorCode.SOLICITUD_INVALIDA,
                        "El parámetro " + dto.getParametroId() + " no pertenece a la plantilla del producto.");
            }
            ResultadoAnalisisMicrobiologico entidad = ResultadoAnalisisMicrobiologico.builder()
                    .evaluacion(evaluacion)
                    .parametro(parametro)
                    .build();
            entidad.setResultado(dto.getResultado());
            entidad.setCumple(ResultadoMicroValidador.calcularCumplimiento(parametro,
                    dto.getResultado(), dto.getCumple()));
            entidad.setObservaciones(dto.getObservaciones());
            aGuardar.add(entidad);
        }

        List<ResultadoAnalisisMicrobiologico> guardados = resultadoRepository.saveAll(aGuardar);
        byte[] pdf = analisisMicroPdfService.generarPdf(evaluacion.getId());
        registrarPdfMicrobiologico(evaluacion, pdf);
        return mapear(guardados);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultadoAnalisisMicroResponseDTO> obtenerPorEvaluacion(Long evaluacionId) {
        return mapear(resultadoRepository.findByEvaluacionId(evaluacionId));
    }

    @Override
    @Transactional
    public byte[] obtenerPdfMicro(Long evaluacionId) {
        EvaluacionCalidad evaluacion = evaluacionRepository.findById(evaluacionId)
                .orElseThrow(() -> new NoSuchElementException("Evaluación no encontrada con ID: " + evaluacionId));

        Optional<ArchivoEvaluacion> adjuntoMicro = buscarAdjuntoMicro(evaluacion);
        if (adjuntoMicro.isPresent()) {
            byte[] contenido = leerArchivoSiExiste(adjuntoMicro.get());
            if (contenido != null) {
                return contenido;
            }
        }

        List<ResultadoAnalisisMicrobiologico> resultados = resultadoRepository.findByEvaluacionId(evaluacionId);
        if (resultados.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "No hay resultados microbiológicos para generar informe");
        }

        byte[] pdf = analisisMicroPdfService.generarPdf(evaluacionId);
        registrarPdfMicrobiologico(evaluacion, pdf);
        return pdf;
    }

    private List<ResultadoAnalisisMicroResponseDTO> mapear(List<ResultadoAnalisisMicrobiologico> entidades) {
        return entidades.stream()
                .map(e -> ResultadoAnalisisMicroResponseDTO.builder()
                        .id(e.getId())
                        .parametroId(e.getParametro().getId())
                        .nombreEnsayo(e.getParametro().getNombreEnsayo())
                        .unidad(e.getParametro().getUnidad())
                        .especificacion(e.getParametro().getEspecificacion())
                        .tipoResultado(e.getParametro().getTipoResultado())
                        .orden(e.getParametro().getOrden())
                        .resultado(e.getResultado())
                        .cumple(e.getCumple())
                        .observaciones(e.getObservaciones())
                        .build())
                .sorted(Comparator.comparing(ResultadoAnalisisMicroResponseDTO::getOrden, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private void registrarPdfMicrobiologico(EvaluacionCalidad evaluacion, byte[] pdf) {
        String nombreArchivo = construirNombreArchivo(evaluacion);

        try {
            Path uploadRoot = obtenerUploadRoot();
            Files.createDirectories(uploadRoot);
            Files.write(uploadRoot.resolve(nombreArchivo), pdf);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo almacenar el PDF microbiológico", e);
        }

        java.util.List<ArchivoEvaluacion> adjuntos = evaluacion.getArchivosAdjuntos();
        if (adjuntos == null) {
            adjuntos = new ArrayList<>();
        } else {
            adjuntos = new ArrayList<>(adjuntos);
        }

        java.util.List<String> archivosRemovidos = new ArrayList<>();
        Iterator<ArchivoEvaluacion> iterator = adjuntos.iterator();
        while (iterator.hasNext()) {
            ArchivoEvaluacion adjunto = iterator.next();
            if (NOMBRE_VISIBLE_MICRO.equalsIgnoreCase(adjunto.getNombreVisible())) {
                archivosRemovidos.add(adjunto.getNombreArchivo());
                iterator.remove();
            }
        }

        archivosRemovidos.forEach(this::eliminarArchivoSiExiste);

        adjuntos.add(ArchivoEvaluacion.builder()
                .nombreArchivo(nombreArchivo)
                .nombreVisible(NOMBRE_VISIBLE_MICRO)
                .build());
        evaluacion.setArchivosAdjuntos(adjuntos);
        evaluacionRepository.save(evaluacion);
    }

    private Optional<ArchivoEvaluacion> buscarAdjuntoMicro(EvaluacionCalidad evaluacion) {
        return Optional.ofNullable(evaluacion.getArchivosAdjuntos())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(a -> NOMBRE_VISIBLE_MICRO.equalsIgnoreCase(a.getNombreVisible()))
                .findFirst();
    }

    private Path obtenerUploadRoot() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones");
    }

    private byte[] leerArchivoSiExiste(ArchivoEvaluacion adjunto) {
        if (adjunto == null || adjunto.getNombreArchivo() == null) {
            return null;
        }
        try {
            Path archivo = obtenerUploadRoot().resolve(adjunto.getNombreArchivo());
            if (!Files.exists(archivo)) {
                return null;
            }
            return Files.readAllBytes(archivo);
        } catch (Exception e) {
            return null;
        }
    }

    private void eliminarArchivoSiExiste(String nombreArchivo) {
        if (nombreArchivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(obtenerUploadRoot().resolve(nombreArchivo));
        } catch (Exception ignored) {
        }
    }

    private String construirNombreArchivo(EvaluacionCalidad evaluacion) {
        String codigoLote = Optional.ofNullable(evaluacion.getLoteProducto())
                .map(l -> l.getCodigoLote())
                .orElse("EVAL_" + evaluacion.getId());
        String codigoSanitizado = codigoLote.replaceAll("[^a-zA-Z0-9._-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return codigoSanitizado + "_MICRO_" + timestamp + ".pdf";
    }
}

