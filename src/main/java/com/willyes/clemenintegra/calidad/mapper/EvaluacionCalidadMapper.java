package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadRequestDTO;
import com.willyes.clemenintegra.calidad.dto.EvaluacionCalidadResponseDTO;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EvaluacionCalidadMapper {

    public EvaluacionCalidad toEntity(EvaluacionCalidadRequestDTO dto,
                                      LoteProducto loteProducto,
                                      Usuario usuarioEvaluador) {
        return EvaluacionCalidad.builder()
                .resultado(dto.getResultado())
                .fechaEvaluacion(LocalDateTime.now()) // Fecha generada automáticamente
                .observaciones(dto.getObservaciones())
                .archivoAdjunto(dto.getArchivoAdjunto())
                .loteProducto(loteProducto)
                .usuarioEvaluador(usuarioEvaluador)
                .build();
    }

    public EvaluacionCalidadResponseDTO toResponseDTO(EvaluacionCalidad entity) {
        return EvaluacionCalidadResponseDTO.builder()
                .id(entity.getId())
                .resultado(entity.getResultado())
                .fechaEvaluacion(entity.getFechaEvaluacion())
                .observaciones(entity.getObservaciones())
                .archivoAdjunto(entity.getArchivoAdjunto())
                .nombreLote(entity.getLoteProducto().getCodigoLote())
                .nombreEvaluador(entity.getUsuarioEvaluador().getNombreCompleto())
                .build();
    }
}


