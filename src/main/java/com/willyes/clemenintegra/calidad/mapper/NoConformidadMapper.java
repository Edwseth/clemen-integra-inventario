package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.NoConformidadDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.inventario.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class NoConformidadMapper {

    public NoConformidadDTO toDTO(NoConformidad entity) {
        return NoConformidadDTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .origen(entity.getOrigen())
                .severidad(entity.getSeveridad())
                .descripcion(entity.getDescripcion())
                .evidencia(entity.getEvidencia())
                .fechaRegistro(entity.getFechaRegistro())
                .usuarioReportaId(entity.getUsuarioReporta().getId())
                .build();
    }

    public NoConformidad toEntity(NoConformidadDTO dto, Usuario usuarioReporta) {
        return NoConformidad.builder()
                .id(dto.getId())
                .codigo(dto.getCodigo())
                .origen(dto.getOrigen())
                .severidad(dto.getSeveridad())
                .descripcion(dto.getDescripcion())
                .evidencia(dto.getEvidencia())
                .fechaRegistro(dto.getFechaRegistro())
                .usuarioReporta(usuarioReporta)
                .build();
    }
}
