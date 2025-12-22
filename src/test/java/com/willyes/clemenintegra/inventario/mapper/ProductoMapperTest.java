package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMapperTest {

    private final ProductoMapper mapper = Mappers.getMapper(ProductoMapper.class);

    @Test
    void exponeAmbosCuandoRequiereQuimicoYMicroSinFisico() {
        Producto producto = Producto.builder()
                .id(10)
                .codigoSku("MP0126")
                .nombre("CHONTADURO")
                .build();
        producto.setRequiereAnalisisFisico(false);
        producto.setRequiereAnalisisQuimico(true);
        producto.setRequiereAnalisisMicrobiologico(true);

        ProductoResponseDTO dto = mapper.toDto(producto);

        assertThat(dto.getTipoAnalisisCalidad()).isEqualTo("AMBOS");
    }
}
