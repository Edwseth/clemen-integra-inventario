package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.LoteProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.LoteProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoteProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaFabricacion", expression = "java(dto.getFechaFabricacion())")
    @Mapping(target = "fechaVencimiento", expression = "java(dto.getFechaVencimiento())")
    @Mapping(target = "fechaLiberacion", expression = "java(dto.getFechaLiberacion())")
    LoteProducto toEntity(LoteProductoRequestDTO dto, Producto producto, Almacen almacen, Usuario usuario);

    // Mapeo de respuesta
    @Mapping(target = "nombreAlmacen", expression = "java(lote.getAlmacen()!=null ? lote.getAlmacen().getNombre() : null)")
    @Mapping(target = "ubicacionAlmacen", expression = "java(lote.getAlmacen()!=null ? lote.getAlmacen().getUbicacion() : null)")
    @Mapping(target = "nombreProducto", expression = "java(lote.getProducto()!=null ? lote.getProducto().getNombre() : null)")
    @Mapping(target = "productoId", expression = "java(lote.getProducto()!=null && lote.getProducto().getId()!=null ? lote.getProducto().getId().longValue() : null)")
    @Mapping(target = "tipoAnalisisCalidad", expression = "java(mapTipoAnalisisCalidadString(lote.getProducto()!=null ? lote.getProducto().getTipoAnalisisCalidad() : null))")
    @Mapping(target = "plantillaMicroId", expression = "java(lote.getProducto()!=null && lote.getProducto().getPlantillaAnalisisMicrobiologico()!=null ? lote.getProducto().getPlantillaAnalisisMicrobiologico().getId() : null)")
    @Mapping(target = "requiereAnalisisMicro", ignore = true)
    @Mapping(target = "requiereAnalisisFisico", expression = "java(lote.getProducto() != null && lote.getProducto().isRequiereAnalisisFisico())")
    @Mapping(target = "requiereAnalisisQuimico", expression = "java(lote.getProducto() != null && lote.getProducto().isRequiereAnalisisQuimico())")
    @Mapping(target = "requiereAnalisisMicrobiologico", expression = "java(lote.getProducto() != null && lote.getProducto().isRequiereAnalisisMicrobiologico())")
    @Mapping(target = "tieneEvaluacionFisica", ignore = true)
    @Mapping(target = "tieneEvaluacionQuimicoMicro", ignore = true)
    @Mapping(target = "evaluacionQuimicoMicroId", ignore = true)
    @Mapping(target = "tieneResultadosMicro", ignore = true)
    @Mapping(target = "pendienteFisico", ignore = true)
    @Mapping(target = "pendienteQuimico", ignore = true)
    @Mapping(target = "pendienteMicro", ignore = true)
    @Mapping(target = "nombreUsuarioLiberador", expression = "java(lote.getUsuarioLiberador()!=null ? lote.getUsuarioLiberador().getNombreCompleto() : null)")
    @Mapping(target = "evaluaciones", ignore = true)
    @Mapping(target = "lotePsOrigenId", expression = "java(lote.getLotePsOrigen()!=null ? lote.getLotePsOrigen().getId() : null)")
    @Mapping(target = "codigoLotePsOrigen", expression = "java(lote.getLotePsOrigen()!=null ? lote.getLotePsOrigen().getCodigoLote() : null)")
    @Mapping(target = "alerta", expression = "java(lote.getAlerta())")
    @Mapping(target = "ubicacionFisicaId", expression = "java(lote.getUbicacionFisica()!=null ? lote.getUbicacionFisica().getId() : null)")
    @Mapping(target = "ubicacionFisicaCodigo", expression = "java(lote.getUbicacionFisica()!=null ? lote.getUbicacionFisica().getCodigo() : null)")
    @Mapping(target = "ubicacionFisicaDescripcion", expression = "java(lote.getUbicacionFisica()!=null ? lote.getUbicacionFisica().getDescripcion() : null)")
    LoteProductoResponseDTO toResponseDTO(LoteProducto lote);

    @Mapping(source = "producto.nombre", target = "nombreProducto")
    @Mapping(target = "productoId", expression = "java(entity.getProducto()!=null && entity.getProducto().getId()!=null ? entity.getProducto().getId().longValue() : null)")
    @Mapping(target = "tipoAnalisisCalidad", expression = "java(mapTipoAnalisisCalidadString(entity.getProducto()!=null ? entity.getProducto().getTipoAnalisisCalidad() : null))")
    @Mapping(target = "plantillaMicroId", expression = "java(entity.getProducto()!=null && entity.getProducto().getPlantillaAnalisisMicrobiologico()!=null ? entity.getProducto().getPlantillaAnalisisMicrobiologico().getId() : null)")
    @Mapping(target = "requiereAnalisisMicro", ignore = true)
    @Mapping(target = "requiereAnalisisFisico", expression = "java(entity.getProducto() != null && entity.getProducto().isRequiereAnalisisFisico())")
    @Mapping(target = "requiereAnalisisQuimico", expression = "java(entity.getProducto() != null && entity.getProducto().isRequiereAnalisisQuimico())")
    @Mapping(target = "requiereAnalisisMicrobiologico", expression = "java(entity.getProducto() != null && entity.getProducto().isRequiereAnalisisMicrobiologico())")
    @Mapping(target = "tieneEvaluacionFisica", ignore = true)
    @Mapping(target = "tieneEvaluacionQuimicoMicro", ignore = true)
    @Mapping(target = "evaluacionQuimicoMicroId", ignore = true)
    @Mapping(target = "tieneResultadosMicro", ignore = true)
    @Mapping(target = "pendienteFisico", ignore = true)
    @Mapping(target = "pendienteQuimico", ignore = true)
    @Mapping(target = "pendienteMicro", ignore = true)
    @Mapping(source = "almacen.nombre", target = "nombreAlmacen")
    @Mapping(source = "almacen.ubicacion", target = "ubicacionAlmacen")
    @Mapping(source = "usuarioLiberador.nombreCompleto", target = "nombreUsuarioLiberador")
    @Mapping(target = "evaluaciones", ignore = true)
    @Mapping(source = "lotePsOrigen.id", target = "lotePsOrigenId")
    @Mapping(source = "lotePsOrigen.codigoLote", target = "codigoLotePsOrigen")
    @Mapping(target = "alerta", expression = "java(entity.getAlerta())")
    @Mapping(target = "ubicacionFisicaId", expression = "java(entity.getUbicacionFisica()!=null ? entity.getUbicacionFisica().getId() : null)")
    @Mapping(target = "ubicacionFisicaCodigo", expression = "java(entity.getUbicacionFisica()!=null ? entity.getUbicacionFisica().getCodigo() : null)")
    @Mapping(target = "ubicacionFisicaDescripcion", expression = "java(entity.getUbicacionFisica()!=null ? entity.getUbicacionFisica().getDescripcion() : null)")
    LoteProductoResponseDTO toDto(LoteProducto entity);

    default String mapTipoAnalisisCalidadString(com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad valor) {
        if (valor == null) {
            return null;
        }
        return switch (valor) {
            case FISICO -> "FISICO_QUIMICO";
            case QUIMICO_MICROBIOLOGICO -> "MICROBIOLOGICO";
            default -> valor.name();
        };
    }

}

