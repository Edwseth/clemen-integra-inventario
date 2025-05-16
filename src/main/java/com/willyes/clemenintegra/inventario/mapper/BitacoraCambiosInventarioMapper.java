package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.BitacoraCambiosInventarioDTO;
import com.willyes.clemenintegra.inventario.model.*;
import org.springframework.stereotype.Component;

@Component
public class BitacoraCambiosInventarioMapper {

    public BitacoraCambiosInventarioDTO toDTO(BitacoraCambiosInventario entity) {
        return BitacoraCambiosInventarioDTO.builder()
                .id(entity.getId())
                .tablaAfectada(entity.getTablaAfectada())
                .registroId(entity.getRegistroId())
                .campoModificado(entity.getCampoModificado())
                .valorAnt(entity.getValorAnt())
                .valorNuevo(entity.getValorNuevo())
                .fechaCambio(entity.getFechaCambio())
                .usuarioId(entity.getUsuario().getId())
                .build();
    }

    public BitacoraCambiosInventario toEntity(BitacoraCambiosInventarioDTO dto) {
        return BitacoraCambiosInventario.builder()
                .id(dto.getId())
                .tablaAfectada(dto.getTablaAfectada())
                .registroId(dto.getRegistroId())
                .campoModificado(dto.getCampoModificado())
                .valorAnt(dto.getValorAnt())
                .valorNuevo(dto.getValorNuevo())
                .fechaCambio(dto.getFechaCambio())
                .usuario(Usuario.builder().id(dto.getUsuarioId()).build())
                .build();
    }
}
