package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadDTO;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class EvaluacionCalidadMapper {

    public EvaluacionCalidadDTO toDTO(EvaluacionCalidad entity) {
        return EvaluacionCalidadDTO.builder()
                .id(entity.getId())
                .resultado(entity.getResultado())
                .fechaEvaluacion(entity.getFechaEvaluacion())
                .observaciones(entity.getObservaciones())
                .archivoAdjunto(entity.getArchivoAdjunto())
                .loteProductoId(entity.getLoteProducto().getId())
                .usuarioEvaluadorId(entity.getUsuarioEvaluador().getId())
                .build();
    }

    public EvaluacionCalidad toEntity(EvaluacionCalidadDTO dto,
                                      LoteProducto loteProducto,
                                      Usuario usuarioEvaluador) {
        return EvaluacionCalidad.builder()
                .id(dto.getId())
                .resultado(dto.getResultado())
                .fechaEvaluacion(dto.getFechaEvaluacion())
                .observaciones(dto.getObservaciones())
                .archivoAdjunto(dto.getArchivoAdjunto())
                .loteProducto(loteProducto)
                .usuarioEvaluador(usuarioEvaluador)
                .build();
    }
}

