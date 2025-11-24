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
    @Mapping(target = "tipoAnalisisCalidad", expression = "java(producto.getTipoAnalisisCalidad() != null ? producto.getTipoAnalisisCalidad().name() : null)")
    @Mapping(target = "rendimiento", source = "rendimientoUnidad")
    @Mapping(target = "unidadMedidaId", expression = "java(producto.getUnidadMedida() != null ? producto.getUnidadMedida().getId() : null)")
    @Mapping(target = "categoriaProductoId", expression = "java(producto.getCategoriaProducto() != null ? producto.getCategoriaProducto().getId() : null)")
    ProductoResponseDTO toDto(Producto producto);

    UnidadMedidaResponseDTO toUnidadMedidaDto(UnidadMedida unidadMedida);

    @Named("mapCategoriaProducto")
    default String mapCategoriaProducto(CategoriaProducto categoriaProducto) {
        return (categoriaProducto != null) ? categoriaProducto.getNombre() : null;
    }

    @Named("mapTipoAnalisisCalidad")
    default TipoAnalisisCalidad mapTipoAnalisisCalidad(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoAnalisisCalidad.NINGUNO;
        }
        String normalizado = valor.trim().toUpperCase();
        return switch (normalizado) {
            case "FISICO_QUIMICO" -> TipoAnalisisCalidad.AMBOS;
            case "MICROBIOLOGICO" -> TipoAnalisisCalidad.QUIMICO_MICROBIOLOGICO;
            default -> TipoAnalisisCalidad.valueOf(normalizado);
        };
    }

    @Named("mapTipoAnalisisCalidadString")
    default String mapTipoAnalisisCalidadString(TipoAnalisisCalidad valor) {
        return valor != null ? valor.name() : null;
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
    default void normalizeScale(@MappingTarget Producto entity) {
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
    }

}


