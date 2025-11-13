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
        LoteProducto lote = entity.getLote();
        Usuario aprobador = entity.getAprobadoPor();
        return RetencionLoteDTO.builder()
                .id(entity.getId())
                .loteId(lote != null ? lote.getId() : null)
                .codigoLote(lote != null ? lote.getCodigoLote() : null)
                .causa(entity.getCausa())
                .fechaRetencion(entity.getFechaRetencion())
                .fechaLiberacion(entity.getFechaLiberacion())
                .estado(entity.getEstado())
                .aprobadoPorId(aprobador != null ? aprobador.getId() : null)
                .aprobadoPorNombre(aprobador != null ? aprobador.getNombreCompleto() : null)
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

