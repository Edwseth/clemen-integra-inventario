package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MrpControllerTest {

    @Test
    void toDtoShouldCalculateCriticidadPerRules() {
        MrpController controller = new MrpController(
                mock(MrpService.class),
                mock(PlanProduccionService.class),
                mock(MrpReporteService.class)
        );

        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(1L)
                .nombre("Categoria Prueba")
                .build();

        Producto producto = Producto.builder()
                .id(1)
                .codigoSku("SKU-1")
                .nombre("Insumo 1")
                .categoriaProducto(categoria)
                .build();

        DetalleCorridaMrp criticidadBaja = DetalleCorridaMrp.builder()
                .id(1L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.TEN)
                .inventarioDisponible(BigDecimal.TEN)
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.ZERO)
                .nivelBom(1)
                .tipoCambioMrp(TipoCambioMrp.NUEVO)
                .build();

        DetalleCorridaMrp criticidadAlta = DetalleCorridaMrp.builder()
                .id(2L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.TEN)
                .inventarioDisponible(BigDecimal.ZERO)
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.valueOf(5))
                .nivelBom(1)
                .tipoCambioMrp(TipoCambioMrp.AUMENTO)
                .build();

        DetalleCorridaMrp criticidadMedia = DetalleCorridaMrp.builder()
                .id(3L)
                .producto(producto)
                .requerimientoBruto(BigDecimal.TEN)
                .inventarioDisponible(BigDecimal.valueOf(2))
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.valueOf(3))
                .nivelBom(1)
                .tipoCambioMrp(TipoCambioMrp.SIN_CAMBIO)
                .build();

        CorridaMrp corrida = CorridaMrp.builder().build();
        corrida.setDetalles(List.of(criticidadBaja, criticidadAlta, criticidadMedia));

        CorridaMrpResponseDTO dto = ReflectionTestUtils.invokeMethod(controller, "toDto", corrida);

        assertNotNull(dto);
        assertNotNull(dto.getDetalles());
        assertEquals(3, dto.getDetalles().size());
        assertEquals("BAJA", dto.getDetalles().get(0).getCriticidad());
        assertEquals("ALTA", dto.getDetalles().get(1).getCriticidad());
        assertEquals("MEDIA", dto.getDetalles().get(2).getCriticidad());
        assertEquals(TipoCambioMrp.NUEVO.name(), dto.getDetalles().get(0).getTipoCambio());
        assertEquals(TipoCambioMrp.AUMENTO.name(), dto.getDetalles().get(1).getTipoCambio());
        assertEquals(TipoCambioMrp.SIN_CAMBIO.name(), dto.getDetalles().get(2).getTipoCambio());
    }
}

