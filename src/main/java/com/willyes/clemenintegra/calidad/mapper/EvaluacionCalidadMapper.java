package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.ArchivoEvaluacionDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionSimpleDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionConsolidadaResponseDTO;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
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
                                                             java.util.List<EvaluacionCalidad> evaluaciones) {
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

        boolean algunNoConforme = evals.stream().anyMatch(e -> e.getResultado() == ResultadoEvaluacion.NO_CONFORME);

        TipoAnalisisCalidad tipoAnalisis = lote.getProducto().getTipoAnalisisCalidad();
        boolean requiereFisico = requiereFisico(lote.getProducto());
        boolean requiereQuimico = requiereQuimico(lote.getProducto());
        boolean requiereMicro = requiereMicro(lote.getProducto());

        boolean completas = (requiereFisico ? fisicoAnyOk : true)
                && ((requiereQuimico || requiereMicro) ? microAnyOk : true);

        String resultadoGlobal;
        if (tipoAnalisis == null) {
            resultadoGlobal = "DESCONOCIDO";
        } else if (tipoAnalisis == TipoAnalisisCalidad.NINGUNO && !requiereFisico && !requiereQuimico && !requiereMicro) {
            resultadoGlobal = "NO_REQUERIDO";
        } else if (algunNoConforme) {
            resultadoGlobal = ResultadoEvaluacion.NO_CONFORME.name();
        } else if ((!requiereFisico || fisicoConforme)
                && (!(requiereQuimico || requiereMicro) || microConforme)) {
            resultadoGlobal = ResultadoEvaluacion.CONFORME.name();
        } else if (completas) {
            resultadoGlobal = ResultadoEvaluacion.CONDICIONADO.name();
        } else {
            boolean avances = (requiereFisico && fisicoCargado)
                    || ((requiereQuimico || requiereMicro) && microCargado);
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
                .evaluacionesRequeridasCompletas(completas)
                .resultadoGlobal(resultadoGlobal)
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
}
