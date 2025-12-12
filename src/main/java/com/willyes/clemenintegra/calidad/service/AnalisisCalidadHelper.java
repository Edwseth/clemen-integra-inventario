package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.model.ArchivoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import java.util.Optional;

import static com.willyes.clemenintegra.calidad.service.ArchivoEvaluacionConstants.NOMBRE_VISIBLE_MICRO;

/**
 * Utilidades para determinar los análisis de calidad requeridos por producto.
 * Siempre basadas en las banderas independientes introducidas para cada disciplina.
 */
public final class AnalisisCalidadHelper {

    private AnalisisCalidadHelper() {
    }

    public static boolean requiereFisico(Producto producto) {
        return producto != null && producto.isRequiereAnalisisFisico();
    }

    public static boolean requiereQuimico(Producto producto) {
        return producto != null && producto.isRequiereAnalisisQuimico();
    }

    public static boolean requiereMicro(Producto producto) {
        return producto != null && producto.isRequiereAnalisisMicrobiologico();
    }

    public static ResultadoValidacionDisciplinas validarDisciplinasCompletas(
            com.willyes.clemenintegra.inventario.model.LoteProducto lote,
            java.util.List<EvaluacionCalidad> evaluaciones,
            java.util.function.LongPredicate existeResultadoMicro) {

        ResultadoValidacionDisciplinas.ResultadoValidacionDisciplinasBuilder builder = ResultadoValidacionDisciplinas.builder();
        if (lote == null || lote.getProducto() == null) {
            return builder.build();
        }

        Producto producto = lote.getProducto();
        java.util.List<EvaluacionCalidad> evals = evaluaciones == null ? java.util.List.of() : evaluaciones;

        boolean requiereFisico = requiereFisico(producto);
        boolean requiereQuimico = requiereQuimico(producto);
        boolean requiereMicro = requiereMicro(producto);

        boolean tieneEvaluacionFisica = evals.stream().anyMatch(e -> e.getTipoEvaluacion() == TipoEvaluacion.FISICO);
        java.util.List<EvaluacionCalidad> evaluacionesQuimicoMicro = evals.stream()
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .toList();

        boolean tieneEvaluacionQuimica = !evaluacionesQuimicoMicro.isEmpty();

        boolean evaluacionMicroConResultados = evaluacionesQuimicoMicro.stream()
                .filter(e -> e.getId() != null)
                .filter(e -> esEvaluacionAprobada(e.getResultado()))
                .anyMatch(e -> existeResultadoMicro != null && existeResultadoMicro.test(e.getId()));

        boolean faltaPdfMicro = false;
        if (requiereMicro && evaluacionMicroConResultados) {
            faltaPdfMicro = evaluacionesQuimicoMicro.stream()
                    .filter(e -> e.getId() != null)
                    .filter(e -> esEvaluacionAprobada(e.getResultado()))
                    .filter(e -> existeResultadoMicro != null && existeResultadoMicro.test(e.getId()))
                    .noneMatch(AnalisisCalidadHelper::tienePdfMicro);
        }

        return builder
                .faltaEvaluacionFisica(requiereFisico && !tieneEvaluacionFisica)
                .faltaEvaluacionQuimica(requiereQuimico && !tieneEvaluacionQuimica)
                .faltanResultadosMicro(requiereMicro && (!tieneEvaluacionQuimica || !evaluacionMicroConResultados))
                .faltaPdfMicro(faltaPdfMicro)
                .build();
    }

    private static boolean esEvaluacionAprobada(ResultadoEvaluacion resultado) {
        return resultado == null
                || resultado == ResultadoEvaluacion.CONFORME
                || resultado == ResultadoEvaluacion.CONDICIONADO;
    }

    private static boolean tienePdfMicro(EvaluacionCalidad evaluacion) {
        if (evaluacion == null || evaluacion.getArchivosAdjuntos() == null) {
            return false;
        }
        return evaluacion.getArchivosAdjuntos().stream()
                .map(ArchivoEvaluacion::getNombreVisible)
                .filter(java.util.Objects::nonNull)
                .anyMatch(nombre -> nombre.equalsIgnoreCase(NOMBRE_VISIBLE_MICRO));
    }

    /**
     * Determina el estado consolidado de las evaluaciones de calidad de un lote.
     * Reglas:
     * - Si ninguna disciplina es requerida -> NO_REQUIERE
     * - Si todas las disciplinas requeridas tienen evaluación registrada -> EVALUADO
     * - En cualquier otro caso -> PENDIENTE
     */
    public static EstadoEvaluacionCalidad calcularEstadoEvaluacion(
            boolean requiereFisico,
            boolean fisicoTieneEvaluacion,
            boolean requiereQuimico,
            boolean quimicoTieneEvaluacion,
            boolean requiereMicro,
            boolean microTieneEvaluacion
    ) {
        if (!requiereFisico && !requiereQuimico && !requiereMicro) {
            return EstadoEvaluacionCalidad.NO_REQUIERE;
        }

        boolean fisicoListo = !requiereFisico || fisicoTieneEvaluacion;
        boolean quimicoListo = !requiereQuimico || quimicoTieneEvaluacion;
        boolean microListo = !requiereMicro || microTieneEvaluacion;

        if (fisicoListo && quimicoListo && microListo) {
            return EstadoEvaluacionCalidad.EVALUADO;
        }

        return EstadoEvaluacionCalidad.PENDIENTE;
    }

    @lombok.Builder
    @lombok.Value
    public static class ResultadoValidacionDisciplinas {
        boolean faltaEvaluacionFisica;
        boolean faltaEvaluacionQuimica;
        boolean faltanResultadosMicro;
        boolean faltaPdfMicro;

        public boolean esValido() {
            return !faltaEvaluacionFisica && !faltaEvaluacionQuimica && !faltanResultadosMicro && !faltaPdfMicro;
        }

        public String getPrimerMensaje() {
            if (faltaEvaluacionFisica) {
                return "Falta evaluación física";
            }
            if (faltaEvaluacionQuimica) {
                return "Falta evaluación química";
            }
            if (faltanResultadosMicro) {
                return "Faltan resultados microbiológicos";
            }
            if (faltaPdfMicro) {
                return "Falta PDF microbiológico";
            }
            return null;
        }
    }
}
