package com.willyes.clemenintegra.calidad.mapper;

import com.willyes.clemenintegra.calidad.dto.CondicionUsoCreateDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.model.CondicionUso;
import com.willyes.clemenintegra.calidad.model.enums.EstadoCondicionUso;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CondicionUsoMapper {

    public CondicionUso toEntity(CondicionUsoCreateDTO dto, LoteProducto lote, Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        return CondicionUso.builder()
                .lote(lote)
                .tipo(dto.getTipo())
                .parametroFecha(dto.getParametroFecha())
                .descripcion(dto.getDescripcion())
                .estado(EstadoCondicionUso.ACTIVA)
                .creadoPor(usuarioId)
                .creadoEn(ahora)
                .build();
    }

    public CondicionUsoResponseDTO toResponseDTO(CondicionUso entity) {
        if (entity == null) {
            return null;
        }
        return CondicionUsoResponseDTO.builder()
                .id(entity.getId())
                .loteId(entity.getLote() != null ? entity.getLote().getId() : null)
                .tipo(entity.getTipo())
                .parametroFecha(entity.getParametroFecha())
                .descripcion(entity.getDescripcion())
                .estado(entity.getEstado())
                .creadoEn(entity.getCreadoEn())
                .creadoPor(entity.getCreadoPor())
                .build();
    }
}
