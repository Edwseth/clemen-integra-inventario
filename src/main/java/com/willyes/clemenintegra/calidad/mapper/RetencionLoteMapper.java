package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.RetencionLoteDTO;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.Usuario;
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
                .aprobadoPor(aprobadoPor)
                .build();
    }
}

