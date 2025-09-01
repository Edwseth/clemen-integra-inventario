package com.willyes.clemenintegra.inventario.mapper;

import com.willyes.clemenintegra.inventario.dto.ProductoResponseDTO;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductoMapperTest {

    private final ProductoMapper mapper = Mappers.getMapper(ProductoMapper.class);

    @Test
    void mapsRendimientoAndUnidadMedida() {
        UnidadMedida unidad = UnidadMedida.builder()
                .id(1L)
                .nombre("Litro")
                .simbolo("L")
                .build();
        Producto producto = new Producto();
        producto.setRendimientoUnidad(new BigDecimal("2.5"));
        producto.setUnidadMedida(unidad);

        ProductoResponseDTO dto = mapper.toDto(producto);

        assertEquals(new BigDecimal("2.5"), dto.getRendimiento());
        assertNotNull(dto.getUnidadMedida());
        assertEquals("L", dto.getUnidadMedida().getSimbolo());
    }
}
