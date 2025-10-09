package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.model.NoConformidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.MotivoRetencion;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class RetencionLoteMapper {

    public RetencionLoteDTO toDTO(RetencionLote entity) {
        return RetencionLoteDTO.builder()
                .id(entity.getId())
                .loteId(entity.getLote().getId())
                .causa(entity.getCausa())
                .fechaRetencion(entity.getFechaRetencion())
                .fechaLiberacion(entity.getFechaLiberacion())
                .estado(entity.getEstado())
                .aprobadoPorId(entity.getAprobadoPor().getId())
                .motivo(entity.getMotivo())
                .noConformidadId(entity.getNoConformidad() != null ? entity.getNoConformidad().getId() : null)
                .build();
    }

    public RetencionLote toEntity(RetencionLoteDTO dto,
                                  LoteProducto lote,
                                  Usuario aprobadoPor) {
        return RetencionLote.builder()
                .id(dto.getId())
                .lote(lote)
                .causa(dto.getCausa())
                .fechaRetencion(dto.getFechaRetencion())
                .fechaLiberacion(dto.getFechaLiberacion())
                .estado(dto.getEstado())
                .motivo(dto.getMotivo() != null ? dto.getMotivo() : MotivoRetencion.OTRO)
                .noConformidad(dto.getNoConformidadId() != null ? NoConformidad.builder().id(dto.getNoConformidadId()).build() : null)
                .aprobadoPor(aprobadoPor)
                .build();
    }
}

