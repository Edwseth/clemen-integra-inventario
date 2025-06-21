package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.MotivoMovimientoResponseDTO;
import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T19:27:31-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class MotivoMovimientoMapperImpl implements MotivoMovimientoMapper {

    @Override
    public MotivoMovimientoResponseDTO toDTO(MotivoMovimiento motivo) {
        if ( motivo == null ) {
            return null;
        }

        MotivoMovimientoResponseDTO motivoMovimientoResponseDTO = new MotivoMovimientoResponseDTO();

        motivoMovimientoResponseDTO.setId( motivo.getId() );
        if ( motivo.getMotivo() != null ) {
            motivoMovimientoResponseDTO.setMotivo( motivo.getMotivo().name() );
        }

        return motivoMovimientoResponseDTO;
    }
}
