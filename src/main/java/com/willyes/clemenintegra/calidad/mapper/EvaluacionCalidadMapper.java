package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionSimpleDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroDetalleDTO;
import com.willyes.clemenintegra.calidad.dto.ResultadoAnalisisMicroResponseDTO;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereFisico;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereMicro;
import static com.willyes.clemenintegra.calidad.service.AnalisisCalidadHelper.requiereQuimico;

@Component
public class EvaluacionCalidadMapper {

    public EvaluacionCalidad toEntity(EvaluacionCalidadRequestDTO dto,
                                      LoteProducto loteProducto,
                                      Usuario usuarioEvaluador) {
        return EvaluacionCalidad.builder()
                .resultado(dto.getResultado())
                .tipoEvaluacion(dto.getTipoEvaluacion())
                .fechaEvaluacion(LocalDateTime.now()) // Fecha generada automáticamente
                .observaciones(dto.getObservaciones())
                .loteProducto(loteProducto)
                .usuarioEvaluador(usuarioEvaluador)
                .build();
    }

    public EvaluacionCalidadResponseDTO toResponseDTO(EvaluacionCalidad entity) {
        if (entity == null) return null;

        return EvaluacionCalidadResponseDTO.builder()
                .id(entity.getId())
                .resultado(entity.getResultado())
                .tipoEvaluacion(entity.getTipoEvaluacion())
                .fechaEvaluacion(entity.getFechaEvaluacion())
                .observaciones(entity.getObservaciones())
                .archivosAdjuntos(
                        entity.getArchivosAdjuntos() == null ? java.util.List.of() :
                                entity.getArchivosAdjuntos().stream()
                                        .map(a -> ArchivoEvaluacionDTO.builder()
                                                .nombreArchivo(a.getNombreArchivo())
                                                .nombreVisible(a.getNombreVisible())
                                                .build())
                                        .toList())
                .nombreLote(entity.getLoteProducto().getCodigoLote())
                .nombreProducto(entity.getLoteProducto().getProducto().getNombre())
                .nombreEvaluador(entity.getUsuarioEvaluador().getNombreCompleto())
                .build();
    }

    public EvaluacionSimpleDTO toSimpleDTO(EvaluacionCalidad entity) {
        if (entity == null) return null;

        return EvaluacionSimpleDTO.builder()
                .tipoEvaluacion(entity.getTipoEvaluacion().name())
                .resultado(entity.getResultado().name())
                .nombreEvaluador(entity.getUsuarioEvaluador().getNombreCompleto())
                .observaciones(entity.getObservaciones())
                .fechaEvaluacion(entity.getFechaEvaluacion())
                .archivosAdjuntos(entity.getArchivosAdjuntos() == null ? java.util.List.of() :
                        entity.getArchivosAdjuntos().stream()
                                .map(a -> ArchivoEvaluacionDTO.builder()
                                        .nombreArchivo(a.getNombreArchivo())
                                        .nombreVisible(a.getNombreVisible())
                                        .build())
                                .toList())
                .build();
    }

    public EvaluacionConsolidadaResponseDTO toConsolidadoDTO(LoteProducto lote,
                                                             java.util.List<EvaluacionCalidad> evaluaciones,
                                                             java.util.Set<Long> evaluacionesConResultadosMicro) {
        if (lote == null) return null;

        java.util.List<EvaluacionCalidad> evals = (evaluaciones == null)
                ? java.util.List.of()
                : evaluaciones;

        java.util.Map<TipoEvaluacion, java.util.List<EvaluacionCalidad>> agrupado = evals.stream()
                .collect(java.util.stream.Collectors.groupingBy(EvaluacionCalidad::getTipoEvaluacion));

        java.util.List<EvaluacionCalidad> fisicos = agrupado.getOrDefault(TipoEvaluacion.FISICO, java.util.List.of());
        java.util.List<EvaluacionCalidad> micros = agrupado.getOrDefault(TipoEvaluacion.QUIMICO_MICROBIOLOGICO, java.util.List.of());

        boolean fisicoCargado = !fisicos.isEmpty();
        boolean microCargado = !micros.isEmpty();

        boolean fisicoConforme = fisicos.stream().anyMatch(e -> e.getResultado() == ResultadoEvaluacion.CONFORME);
        boolean microConforme = micros.stream().anyMatch(e -> e.getResultado() == ResultadoEvaluacion.CONFORME);

        boolean fisicoAnyOk = fisicos.stream().anyMatch(e ->
                e.getResultado() == ResultadoEvaluacion.CONFORME ||
                        e.getResultado() == ResultadoEvaluacion.CONDICIONADO);

        boolean microAnyOk = micros.stream().anyMatch(e ->
                e.getResultado() == ResultadoEvaluacion.CONFORME ||
                        e.getResultado() == ResultadoEvaluacion.CONDICIONADO);

        boolean tieneResultadosMicro = micros.stream()
                .map(EvaluacionCalidad::getId)
                .anyMatch(id -> evaluacionesConResultadosMicro != null && evaluacionesConResultadosMicro.contains(id));

        boolean tieneAdjuntosFisico = fisicos.stream()
                .anyMatch(e -> e.getArchivosAdjuntos() != null && !e.getArchivosAdjuntos().isEmpty());
        boolean tieneAdjuntosQuimicoMicro = micros.stream()
                .anyMatch(e -> e.getArchivosAdjuntos() != null && !e.getArchivosAdjuntos().isEmpty());

        boolean algunNoConforme = evals.stream().anyMatch(e -> e.getResultado() == ResultadoEvaluacion.NO_CONFORME);

        TipoAnalisisCalidad tipoAnalisis = lote.getProducto().getTipoAnalisisCalidad();
        boolean requiereFisico = requiereFisico(lote.getProducto());
        boolean requiereQuimico = requiereQuimico(lote.getProducto());
        boolean requiereMicro = requiereMicro(lote.getProducto());

        boolean requiereQuimicoOMicro = requiereQuimico || requiereMicro;

        boolean disciplinasCompletas = (!requiereFisico || fisicoCargado)
                && (!requiereQuimicoOMicro || microCargado)
                && (!requiereMicro || tieneResultadosMicro);

        boolean evaluacionesCompletas = (!requiereFisico || fisicoAnyOk)
                && (!requiereQuimicoOMicro || microAnyOk)
                && (!requiereMicro || tieneResultadosMicro);

        String resultadoGlobal;
        if (tipoAnalisis == null) {
            resultadoGlobal = "DESCONOCIDO";
        } else if (tipoAnalisis == TipoAnalisisCalidad.NINGUNO && !requiereFisico && !requiereQuimico && !requiereMicro) {
            resultadoGlobal = "NO_REQUERIDO";
        } else if (algunNoConforme) {
            resultadoGlobal = ResultadoEvaluacion.NO_CONFORME.name();
        } else if ((!requiereFisico || fisicoConforme)
                && (!requiereQuimicoOMicro || (microConforme && (!requiereMicro || tieneResultadosMicro)))) {
            resultadoGlobal = ResultadoEvaluacion.CONFORME.name();
        } else if (evaluacionesCompletas) {
            resultadoGlobal = ResultadoEvaluacion.CONDICIONADO.name();
        } else {
            boolean avances = (requiereFisico && fisicoCargado)
                    || (requiereQuimicoOMicro && microCargado)
                    || (requiereMicro && tieneResultadosMicro);
            resultadoGlobal = avances ? "EN_PROCESO" : "PENDIENTE";
        }

        return EvaluacionConsolidadaResponseDTO.builder()
                .id(lote.getId())
                .nombreLote(lote.getCodigoLote())
                .nombreProducto(lote.getProducto().getNombre())
                .estadoLote(lote.getEstado().name())
                .tipoAnalisisCalidad(tipoAnalisis != null ? tipoAnalisis.name() : null)
                .tipoAnalisisRequerido(mapearAnalisisRequerido(tipoAnalisis))
                .fisicoQuimicoCargado(fisicoCargado)
                .microbiologicoCargado(microCargado)
                .evaluacionesRequeridasCompletas(disciplinasCompletas)
                .resultadoGlobal(resultadoGlobal)
                .requiereAnalisisFisico(requiereFisico)
                .requiereAnalisisQuimico(requiereQuimico)
                .requiereAnalisisMicrobiologico(requiereMicro)
                .tieneEvaluacionFisica(fisicoCargado)
                .tieneEvaluacionQuimicaMicro(microCargado)
                .tieneResultadosMicro(tieneResultadosMicro)
                .tieneAdjuntosFisico(tieneAdjuntosFisico)
                .tieneAdjuntosQuimicoMicro(tieneAdjuntosQuimicoMicro)
                .evaluaciones(evals.stream()
                        .map(this::toSimpleDTO)
                        .toList())
                .build();
    }

    private String mapearAnalisisRequerido(TipoAnalisisCalidad tipoAnalisis) {
        if (tipoAnalisis == null) {
            return null;
        }
        return switch (tipoAnalisis) {
            case FISICO -> "FISICO_QUIMICO";
            case QUIMICO_MICROBIOLOGICO -> "MICROBIOLOGICO";
            default -> tipoAnalisis.name();
        };
    }

    public EvaluacionCalidadDetalleDTO toDetalleDTO(EvaluacionCalidad evaluacion,
                                                    Producto producto,
                                                    java.util.List<ResultadoAnalisisMicroDetalleDTO> resultadosMicro) {
        if (evaluacion == null || producto == null) {
            return null;
        }

        boolean esFisico = evaluacion.getTipoEvaluacion() == TipoEvaluacion.FISICO;
        boolean esQuimicoMicro = evaluacion.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO;

        return EvaluacionCalidadDetalleDTO.builder()
                .idEvaluacion(evaluacion.getId())
                .loteId(evaluacion.getLoteProducto().getId())
                .codigoLote(evaluacion.getLoteProducto().getCodigoLote())
                .nombreProducto(producto.getNombre())
                .tipoEvaluacion(evaluacion.getTipoEvaluacion().name())
                .fechaEvaluacion(evaluacion.getFechaEvaluacion())
                .resultado(evaluacion.getResultado() != null ? evaluacion.getResultado().name() : null)
                .observaciones(evaluacion.getObservaciones())
                .requiereAnalisisFisico(requiereFisico(producto))
                .requiereAnalisisQuimico(requiereQuimico(producto))
                .requiereAnalisisMicrobiologico(requiereMicro(producto))
                .tieneResultadosFisicos(esFisico)
                .tieneResultadosQuimicos(esQuimicoMicro)
                .tieneResultadosMicro(resultadosMicro != null && !resultadosMicro.isEmpty())
                .resultadosMicro(resultadosMicro == null ? java.util.List.of() : resultadosMicro)
                .archivosAdjuntos(mapearArchivos(evaluacion))
                .build();
    }

    public java.util.List<ResultadoAnalisisMicroDetalleDTO> mapearResultadosMicro(java.util.List<ResultadoAnalisisMicroResponseDTO> resultados) {
        if (resultados == null) {
            return java.util.List.of();
        }
        return resultados.stream()
                .map(r -> ResultadoAnalisisMicroDetalleDTO.builder()
                        .parametroId(r.getParametroId())
                        .nombreEnsayo(r.getNombreEnsayo())
                        .especificacion(r.getEspecificacion())
                        .unidad(r.getUnidad())
                        .resultado(r.getResultado())
                        .cumple(r.getCumple())
                        .observaciones(r.getObservaciones())
                        .build())
                .toList();
    }

    private java.util.List<ArchivoEvaluacionDTO> mapearArchivos(EvaluacionCalidad evaluacion) {
        return evaluacion.getArchivosAdjuntos() == null ? java.util.List.of() :
                evaluacion.getArchivosAdjuntos().stream()
                        .map(a -> ArchivoEvaluacionDTO.builder()
                                .nombreArchivo(a.getNombreArchivo())
                                .nombreVisible(a.getNombreVisible())
                                .build())
                        .toList();
    }
}
