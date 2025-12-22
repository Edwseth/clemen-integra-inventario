package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoRequestDTO;
import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.dto.UnidadMedidaResponseDTO;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.TipoAnalisisCalidad;
import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductoMapper {

    Logger LOG = LoggerFactory.getLogger(ProductoMapper.class);

    default ProductoResponseDTO safeToDto(Producto producto) {
        if (producto == null) {
            LOG.warn("Producto nulo detectado al mapear a DTO");
            return null;
        }
        return toDto(producto);
    }

    @Mapping(source = "codigoSku", target = "sku")
    @Mapping(target = "unidadMedida", source = "unidadMedida")
    @Mapping(target = "categoria", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getNombre() : null)")
    @Mapping(target = "tipoAnalisisCalidad", expression = "java(mapTipoAnalisisCalidadStringDesdeFlags(producto))")
    @Mapping(target = "rendimiento", source = "rendimientoUnidad")
    @Mapping(target = "unidadMedidaId", expression = "java(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getId() : null)")
    @Mapping(target = "categoriaProductoId", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getId() : null)")
    @Mapping(target = "plantillaAnalisisMicroId", expression = "java(producto.getPlantillaAnalisisMicrobiologico() != null ? producto.getPlantillaAnalisisMicrobiologico().getId() : null)")
    @Mapping(target = "plantillaAnalisisMicroNombre", expression = "java(producto.getPlantillaAnalisisMicrobiologico() != null ? producto.getPlantillaAnalisisMicrobiologico().getNombre() : null)")
    @Mapping(target = "requiereAnalisisFisico", source = "requiereAnalisisFisico")
    @Mapping(target = "requiereAnalisisQuimico", source = "requiereAnalisisQuimico")
    @Mapping(target = "requiereAnalisisMicrobiologico", source = "requiereAnalisisMicrobiologico")
    ProductoResponseDTO toDto(Producto producto);

    UnidadMedidaResponseDTO toUnidadMedidaDto(UnidadMedida unidadMedida);

    @Named("mapCategoriaProducto")
    default String mapCategoriaProducto(CategoriaProducto categoriaProducto) {
        return (categoriaProducto != null) ? categoriaProducto.getNombre() : null;
    }

    default String mapTipoAnalisisCalidadStringDesdeFlags(Producto producto) {
        if (producto == null) {
            return null;
        }
        TipoAnalisisCalidad derivado = TipoAnalisisCalidad.fromFlags(
                producto.isRequiereAnalisisFisico(),
                producto.isRequiereAnalisisQuimico(),
                producto.isRequiereAnalisisMicrobiologico()
        );
        // LEGACY: exposiciones actuales siguen usando el enum derivado
        return mapTipoAnalisisCalidadString(derivado);
    }

    @Named("mapTipoAnalisisCalidad")
    default TipoAnalisisCalidad mapTipoAnalisisCalidad(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoAnalisisCalidad.NINGUNO;
        }
        String normalizado = valor.trim().toUpperCase();
        return switch (normalizado) {
            case "FISICO", "FISICO_QUIMICO" -> TipoAnalisisCalidad.FISICO;
            case "MICROBIOLOGICO", "QUIMICO_MICROBIOLOGICO" -> TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO;
            default -> TipoAnalisisCalidad.valueOf(normalizado);
        };
    }

    @Named("mapTipoAnalisisCalidadString")
    default String mapTipoAnalisisCalidadString(TipoAnalisisCalidad valor) {
        if (valor == null) {
            return null;
        }
        return switch (valor) {
            case FISICO -> "FISICO_QUIMICO";
            case QUIMICO_MICROBIOLOGICO -> "AMBOS";
            default -> valor.name();
        };
    }

    @AfterMapping
    default void setDefaultRendimiento(@MappingTarget ProductoResponseDTO dto) {
        if (dto.getRendimiento() == null) {
            dto.setRendimiento(BigDecimal.ZERO);
        }
    }

    // ====== NUEVOS: request -> entidad / update parcial ======
    Producto toEntity(ProductoRequestDTO dto);

    void update(@MappingTarget Producto entity, ProductoRequestDTO dto);

    @AfterMapping
    default void normalizeScale(@MappingTarget Producto entity, ProductoRequestDTO dto) {
        if (entity.getRendimientoUnidad() != null) {
            entity.setRendimientoUnidad(
                    entity.getRendimientoUnidad().setScale(2, RoundingMode.HALF_UP)
            );
        }
        if (entity.getStockSeguridad() != null) {
            entity.setStockSeguridad(entity.getStockSeguridad().setScale(6, RoundingMode.HALF_UP));
        }
        if (entity.getStockMaximoPlaneacion() != null) {
            entity.setStockMaximoPlaneacion(entity.getStockMaximoPlaneacion().setScale(6, RoundingMode.HALF_UP));
        }

        boolean flagsPresentes = dto.getRequiereAnalisisFisico() != null
                || dto.getRequiereAnalisisQuimico() != null
                || dto.getRequiereAnalisisMicrobiologico() != null;

        boolean requiereFisico = Boolean.TRUE.equals(dto.getRequiereAnalisisFisico());
        boolean requiereQuimico = Boolean.TRUE.equals(dto.getRequiereAnalisisQuimico());
        boolean requiereMicro = Boolean.TRUE.equals(dto.getRequiereAnalisisMicrobiologico());

        if (!flagsPresentes) {
            // LEGACY: compatibilidad con clientes que aún envían solo tipoAnalisisCalidad
            TipoAnalisisCalidad legado = mapTipoAnalisisCalidad(dto.getTipoAnalisisCalidad());
            requiereFisico = legado == TipoAnalisisCalidad.FISICO || legado == TipoAnalisisCalidad.AMBOS;
            requiereQuimico = legado == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || legado == TipoAnalisisCalidad.AMBOS;
            requiereMicro = legado == TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO || legado == TipoAnalisisCalidad.AMBOS;
        }

        entity.setRequiereAnalisisFisico(requiereFisico);
        entity.setRequiereAnalisisQuimico(requiereQuimico);
        entity.setRequiereAnalisisMicrobiologico(requiereMicro);
        entity.recomputarTipoAnalisisDesdeBanderas();
    }

}

