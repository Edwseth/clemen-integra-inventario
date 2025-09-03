package com.willyes.clemenintegra.produccion.mapper;

import com.willyes.clemenintegra.produccion.dto.EtapaPlantillaRequest;
import com.willyes.clemenintegra.produccion.dto.EtapaPlantillaResponse;
import com.willyes.clemenintegra.produccion.model.EtapaPlantilla;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EtapaPlantillaMapper {
    EtapaPlantilla toEntity(EtapaPlantillaRequest request);

    @Mapping(target = "productoId", source = "producto.id")
    EtapaPlantillaResponse toResponse(EtapaPlantilla entity);
}
