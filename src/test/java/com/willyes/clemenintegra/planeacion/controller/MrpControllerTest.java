package com.willyes.clemenintegra.planeacion.controller;

import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.planeacion.dto.CorridaMrpResponseDTO;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.service.MrpReporteService;
import com.willyes.clemenintegra.planeacion.service.MrpService;
import com.willyes.clemenintegra.planeacion.service.impl.MrpServiceImpl;
import com.willyes.clemenintegra.planeacion.service.PlanProduccionService;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MrpControllerTest {

    private static final UnidadMedida UNIDAD_MEDIDA_BASE = UnidadMedida.builder()
            .id(1L)
            .codigo("UND")
            .nombre("UNIDAD")
            .simbolo("UND")
            .build();

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
                .unidadMedida(UNIDAD_MEDIDA_BASE)
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

    @Test
    void obtenerDebeEnriquecerMetricasParaGet() {
        FormulaProductoRepository formulaProductoRepository = mock(FormulaProductoRepository.class);
        LoteProductoRepository loteProductoRepository = mock(LoteProductoRepository.class);
        OrdenCompraDetalleRepository ordenCompraDetalleRepository = mock(OrdenCompraDetalleRepository.class);
        CorridaMrpRepository corridaMrpRepository = mock(CorridaMrpRepository.class);
        MrpServiceImpl service = new MrpServiceImpl(
                formulaProductoRepository,
                loteProductoRepository,
                ordenCompraDetalleRepository,
                corridaMrpRepository
        );
        MrpController controller = new MrpController(
                service,
                mock(PlanProduccionService.class),
                mock(MrpReporteService.class)
        );

        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(2L)
                .nombre("Materia Prima")
                .build();
        Producto producto = Producto.builder()
                .id(10)
                .codigoSku("SKU-PS-1")
                .nombre("Producto Semielaborado")
                .categoriaProducto(categoria)
                .leadTimeCompraDias(7)
                .unidadMedida(UNIDAD_MEDIDA_BASE)
                .build();

        CorridaMrp corrida = CorridaMrp.builder()
                .id(99L)
                .horizonteInicio(java.time.LocalDate.of(2024, 1, 1))
                .horizonteFin(java.time.LocalDate.of(2024, 1, 14))
                .detalles(new java.util.ArrayList<>())
                .build();
        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .id(100L)
                .corrida(corrida)
                .producto(producto)
                .requerimientoBruto(BigDecimal.valueOf(50))
                .inventarioDisponible(BigDecimal.valueOf(10))
                .recepcionesProgramadas(BigDecimal.valueOf(5))
                .requerimientoNeto(BigDecimal.valueOf(20))
                .nivelBom(1)
                .build();
        corrida.getDetalles().add(detalle);
        when(corridaMrpRepository.findWithGraphById(99L)).thenReturn(Optional.of(corrida));

        ResponseEntity<?> response = controller.obtener(99L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof CorridaMrpResponseDTO);
        CorridaMrpResponseDTO dto = (CorridaMrpResponseDTO) response.getBody();
        assertNotNull(dto.getDetalles());
        assertEquals(1, dto.getDetalles().size());
        CorridaMrpResponseDTO.DetalleCorridaMrpDTO detalleDto = dto.getDetalles().get(0);
        assertNotNull(detalleDto.getConsumoSemanalPromedio());
        assertNotNull(detalleDto.getSemanasCobertura());

        assertNotNull(dto.getSugerencias());
        assertEquals(1, dto.getSugerencias().size());
        CorridaMrpResponseDTO.SugerenciaAbastecimientoDTO sugerenciaDto = dto.getSugerencias().get(0);
        assertNotNull(sugerenciaDto.getConsumoSemanalPromedio());
        assertNotNull(sugerenciaDto.getSemanasCobertura());
        assertNotNull(sugerenciaDto.getNivelCriticidad());
        assertNotNull(sugerenciaDto.getEsCritico());
    }

    @Test
    void obtenerDebeRetornarRazonesCriticidadParaAltoYCritico() {
        FormulaProductoRepository formulaProductoRepository = mock(FormulaProductoRepository.class);
        LoteProductoRepository loteProductoRepository = mock(LoteProductoRepository.class);
        OrdenCompraDetalleRepository ordenCompraDetalleRepository = mock(OrdenCompraDetalleRepository.class);
        CorridaMrpRepository corridaMrpRepository = mock(CorridaMrpRepository.class);
        MrpServiceImpl service = new MrpServiceImpl(
                formulaProductoRepository,
                loteProductoRepository,
                ordenCompraDetalleRepository,
                corridaMrpRepository
        );
        MrpController controller = new MrpController(
                service,
                mock(PlanProduccionService.class),
                mock(MrpReporteService.class)
        );

        CorridaMrp corrida = CorridaMrp.builder()
                .id(100L)
                .horizonteInicio(java.time.LocalDate.of(2024, 1, 1))
                .horizonteFin(java.time.LocalDate.of(2024, 1, 14))
                .detalles(new java.util.ArrayList<>())
                .build();

        CategoriaProducto categoria = CategoriaProducto.builder()
                .id(3L)
                .nombre("Categoria 3")
                .build();
        Producto productoAlto = Producto.builder()
                .id(50)
                .codigoSku("SKU-ALTO")
                .nombre("Producto Alto")
                .categoriaProducto(categoria)
                .leadTimeCompraDias(7)
                .unidadMedida(UNIDAD_MEDIDA_BASE)
                .build();
        DetalleCorridaMrp detalleAlto = DetalleCorridaMrp.builder()
                .id(101L)
                .corrida(corrida)
                .producto(productoAlto)
                .requerimientoBruto(BigDecimal.valueOf(50))
                .inventarioDisponible(BigDecimal.valueOf(40))
                .recepcionesProgramadas(BigDecimal.valueOf(10))
                .requerimientoNeto(BigDecimal.valueOf(5))
                .nivelBom(1)
                .build();

        Producto productoCritico = Producto.builder()
                .id(60)
                .codigoSku("SKU-CRITICO")
                .nombre("Producto Critico")
                .categoriaProducto(categoria)
                .leadTimeCompraDias(21)
                .unidadMedida(UNIDAD_MEDIDA_BASE)
                .build();
        DetalleCorridaMrp detalleCritico = DetalleCorridaMrp.builder()
                .id(102L)
                .corrida(corrida)
                .producto(productoCritico)
                .requerimientoBruto(BigDecimal.valueOf(50))
                .inventarioDisponible(BigDecimal.valueOf(5))
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.valueOf(45))
                .nivelBom(1)
                .build();

        corrida.getDetalles().add(detalleAlto);
        corrida.getDetalles().add(detalleCritico);
        when(corridaMrpRepository.findWithGraphById(100L)).thenReturn(Optional.of(corrida));

        ResponseEntity<?> response = controller.obtener(100L);
        CorridaMrpResponseDTO dto = (CorridaMrpResponseDTO) response.getBody();

        assertNotNull(dto);
        assertEquals(2, dto.getDetalles().size());
        CorridaMrpResponseDTO.DetalleCorridaMrpDTO detalleDtoAlto = dto.getDetalles().get(0);
        assertNotNull(detalleDtoAlto.getRazonesCriticidad());
        assertEquals("ALTO", detalleDtoAlto.getCriticidad());
        assertTrue(detalleDtoAlto.getRazonesCriticidad().contains("COBERTURA_MENOR_A_3_SEMANAS"));

        CorridaMrpResponseDTO.DetalleCorridaMrpDTO detalleDtoCritico = dto.getDetalles().get(1);
        assertNotNull(detalleDtoCritico.getRazonesCriticidad());
        assertEquals("CRITICO", detalleDtoCritico.getCriticidad());
        assertTrue(detalleDtoCritico.getRazonesCriticidad().contains("COBERTURA_MENOR_A_1_SEMANA"));
        assertTrue(detalleDtoCritico.getRazonesCriticidad().contains("COBERTURA_MENOR_A_LEAD_TIME"));
    }
}
