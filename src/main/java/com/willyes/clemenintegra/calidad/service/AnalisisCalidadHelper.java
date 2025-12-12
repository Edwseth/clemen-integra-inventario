package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import java.util.Optional;

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
        java.util.Optional<EvaluacionCalidad> evaluacionQuimicaMicro = evals.stream()
                .filter(e -> e.getTipoEvaluacion() == TipoEvaluacion.QUIMICO_MICROBIOLOGICO)
                .findFirst();

        boolean tieneEvaluacionQuimica = evaluacionQuimicaMicro.isPresent();
        boolean tieneResultadosMicro = evaluacionQuimicaMicro
                .map(EvaluacionCalidad::getId)
                .filter(id -> existeResultadoMicro != null)
                .map(existeResultadoMicro::test)
                .orElse(false);

        return builder
                .faltaEvaluacionFisica(requiereFisico && !tieneEvaluacionFisica)
                .faltaEvaluacionQuimica(requiereQuimico && !tieneEvaluacionQuimica)
                .faltanResultadosMicro(requiereMicro && (!tieneEvaluacionQuimica || !tieneResultadosMicro))
                .build();
    }

    @lombok.Builder
    @lombok.Value
    public static class ResultadoValidacionDisciplinas {
        boolean faltaEvaluacionFisica;
        boolean faltaEvaluacionQuimica;
        boolean faltanResultadosMicro;

        public boolean esValido() {
            return !faltaEvaluacionFisica && !faltaEvaluacionQuimica && !faltanResultadosMicro;
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
            return null;
        }
    }
}
