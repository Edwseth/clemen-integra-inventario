package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ConsolidadoPorLoteDTO {

    private final Long loteId;
    private final String codigoLote;
    private final String nombreProducto;
    private final String tipoAnalisisCalidad;
    private final EstadoLote estadoLote;
    private final DisciplinaConsolidadoDTO fisico;
    private final DisciplinaConsolidadoDTO quimicoMicrobiologico;
    private final DisciplinaConsolidadoDTO microbiologico;
    private final boolean requiereAnalisisFisico;
    private final boolean requiereAnalisisQuimico;
    private final boolean requiereAnalisisMicrobiologico;
    private final Boolean fisicoConforme;
    private final Boolean quimicoConforme;
    private final Boolean microConforme;
    private final String estadoFisico;
    private final String estadoQuimico;
    private final String estadoMicro;
    private final Long evaluacionFisicaId;
    private final Long evaluacionQuimicoMicroId;
    private final Long evaluacionMicroId;
    private final boolean liberable;
    private final boolean faltanEvaluaciones;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DisciplinaConsolidadoDTO {
        private final boolean requerido;
        private final String estado;
        private final String resultado;
        private final LocalDateTime fechaUltimaEvaluacion;
        private final String evaluador;
    }
}
