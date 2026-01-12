package com.willyes.clemenintegra.calidad.dto;

import com.willyes.clemenintegra.calidad.model.enums.EstadoEvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion;
import com.willyes.clemenintegra.calidad.model.enums.TipoEvaluacion;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EvaluacionConsolidadaListadoDTO {

    private final Long id;
    private final Long loteId;
    private final LocalDateTime fechaEvaluacion;
    private final String codigoLote;
    private final String nombreProducto;
    private final ResultadoEvaluacion resultado;
    private final TipoEvaluacion tipoEvaluacion;
    private final String usuarioCreador;
    private final EstadoEvaluacionCalidad estadoEvaluacion;
    private final EstadoLote estadoLote;
    private final long cantidadAdjuntos;
    private final boolean tieneAdjuntos;

    public EvaluacionConsolidadaListadoDTO(Long id,
                                           Long loteId,
                                           LocalDateTime fechaEvaluacion,
                                           String codigoLote,
                                           String nombreProducto,
                                           ResultadoEvaluacion resultado,
                                           TipoEvaluacion tipoEvaluacion,
                                           String usuarioCreador,
                                           EstadoEvaluacionCalidad estadoEvaluacion,
                                           EstadoLote estadoLote,
                                           Long cantidadAdjuntos) {
        this.id = id;
        this.loteId = loteId;
        this.fechaEvaluacion = fechaEvaluacion;
        this.codigoLote = codigoLote;
        this.nombreProducto = nombreProducto;
        this.resultado = resultado;
        this.tipoEvaluacion = tipoEvaluacion;
        this.usuarioCreador = usuarioCreador;
        this.estadoEvaluacion = estadoEvaluacion;
        this.estadoLote = estadoLote;
        this.cantidadAdjuntos = cantidadAdjuntos != null ? cantidadAdjuntos : 0L;
        this.tieneAdjuntos = this.cantidadAdjuntos > 0;
    }
}
