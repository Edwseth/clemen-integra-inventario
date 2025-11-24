package com.willyes.clemenintegra.planeacion.mapper;

import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.dto.SugerenciaAbastecimientoResponseDTO;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlaneacionMrpMapper {

    @Mapping(target = "corridaId", source = "corrida.id")
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoSku", expression = "java(entidad.getProducto() != null ? entidad.getProducto().getCodigoSku() : null)")
    @Mapping(target = "productoNombre", expression = "java(entidad.getProducto() != null ? entidad.getProducto().getNombre() : null)")
    @Mapping(target = "categoriaProducto", expression = "java(entidad.getProducto() != null && entidad.getProducto().getCategoriaProducto() != null ? entidad.getProducto().getCategoriaProducto().getNombre() : null)")
    @Mapping(target = "tipoSugerencia", expression = "java(entidad.getTipo() != null ? entidad.getTipo().name() : null)")
    @Mapping(target = "estado", expression = "java(entidad.getEstado() != null ? entidad.getEstado().name() : null)")
    @Mapping(target = "origen", expression = "java(entidad.getOrigen() != null ? entidad.getOrigen().name() : null)")
    SugerenciaAbastecimientoResponseDTO toDto(SugerenciaAbastecimiento entidad);

    List<SugerenciaAbastecimientoResponseDTO> toDtos(List<SugerenciaAbastecimiento> entidades);

    @Mapping(target = "modo", expression = "java(\"MRP_SIMPLE\")")
    @Mapping(target = "totalSugerencias", expression = "java(corrida.getSugerencias() != null ? corrida.getSugerencias().size() : 0)")
    @Mapping(target = "sugerencias", source = "sugerencias")
    CorridaMrpResponseDTO toDto(CorridaMrp corrida);

    default CorridaMrpResponseDTO toDtoSinSugerencias(CorridaMrp corrida) {
        if (corrida == null) {
            return null;
        }
        CorridaMrpResponseDTO dto = toDto(corrida);
        dto.setSugerencias(Collections.emptyList());
        return dto;
    }
}
