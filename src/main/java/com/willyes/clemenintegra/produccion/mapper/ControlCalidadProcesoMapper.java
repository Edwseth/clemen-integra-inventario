package com.willyes.clemenintegra.produccion.mapper;

import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.dto.ControlCalidadProcesoRequest;
import com.willyes.clemenintegra.produccion.dto.ControlCalidadProcesoResponse;
import com.willyes.clemenintegra.produccion.model.ControlCalidadProceso;
import com.willyes.clemenintegra.produccion.model.DetalleEtapa;

public class ControlCalidadProcesoMapper {

    public static ControlCalidadProceso toEntity(ControlCalidadProcesoRequest request, DetalleEtapa detalle, Usuario evaluador) {
        return ControlCalidadProceso.builder()
                .parametro(request.parametro)
                .valorMedido(request.valorMedido)
                .cumple(request.cumple)
                .observaciones(request.observaciones)
                .detalleEtapa(detalle)
                .evaluador(evaluador)
                .build();
    }

    public static ControlCalidadProcesoResponse toResponse(ControlCalidadProceso entity) {
        ControlCalidadProcesoResponse response = new ControlCalidadProcesoResponse();
        response.id = entity.getId();
        response.parametro = entity.getParametro();
        response.valorMedido = entity.getValorMedido();
        response.cumple = entity.getCumple();
        response.observaciones = entity.getObservaciones();
        response.detalleEtapaId = entity.getDetalleEtapa() != null ? entity.getDetalleEtapa().getId() : null;
        response.evaluadorNombre = entity.getEvaluador() != null ? entity.getEvaluador().getNombreCompleto() : null;
        return response;
    }
}

