package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroRequestDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
            entidad.setCumple(dto.getCumple());
            entidad.setObservaciones(dto.getObservaciones());
            aGuardar.add(entidad);
        }

        List<ResultadoAnalisisMicrobiologico> guardados = resultadoRepository.saveAll(aGuardar);
        registrarPdfMicrobiologico(evaluacion);
        return mapear(guardados);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultadoAnalisisMicroResponseDTO> obtenerPorEvaluacion(Long evaluacionId) {
        return mapear(resultadoRepository.findByEvaluacionId(evaluacionId));
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

    private void registrarPdfMicrobiologico(EvaluacionCalidad evaluacion) {
        byte[] pdf = analisisMicroPdfService.generarPdf(evaluacion.getId());
        String nombreArchivo = System.currentTimeMillis() + "_MICRO_" + evaluacion.getId() + ".pdf";

        try {
            Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "evaluaciones");
            Files.createDirectories(uploadRoot);
            Files.write(uploadRoot.resolve(nombreArchivo), pdf);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo almacenar el PDF microbiológico", e);
        }

        java.util.List<com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion> adjuntos = evaluacion.getArchivosAdjuntos();
        if (adjuntos == null) {
            adjuntos = new ArrayList<>();
        } else {
            adjuntos = new ArrayList<>(adjuntos);
        }
        adjuntos.removeIf(a -> "Microbiológico".equalsIgnoreCase(a.getNombreVisible()));
        adjuntos.add(com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion.builder()
                .nombreArchivo(nombreArchivo)
                .nombreVisible("Microbiológico")
                .build());
        evaluacion.setArchivosAdjuntos(adjuntos);
        evaluacionRepository.save(evaluacion);
    }
}

