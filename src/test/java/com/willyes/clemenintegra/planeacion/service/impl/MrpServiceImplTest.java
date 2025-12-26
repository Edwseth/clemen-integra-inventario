package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import com.willyes.clemenintegra.planeacion.model.enums.TipoSugerenciaAbastecimiento;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MrpServiceImplTest {

    @Mock
    private FormulaProductoRepository formulaProductoRepository;

    @Mock
    private LoteProductoRepository loteProductoRepository;

    @Mock
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;

    @Mock
    private CorridaMrpRepository corridaMrpRepository;

    @InjectMocks
    @Spy
    private MrpServiceImpl service;

    @Test
    void noPermiteEjecutarMrpConPlanBorrador() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .estado(EstadoPlanProduccion.BORRADOR)
                .build();

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorridaSemana(plan));
    }

    @Test
    void noPermiteEjecutarMrpConPlanCerrado() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .estado(EstadoPlanProduccion.CERRADO)
                .build();

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorridaSemana(plan));
    }

    @Test
    void calculaRequerimientoNetoEnCeroCuandoInventarioCubreBruto() {
        Producto producto = Producto.builder().id(1).build();
        Map<Producto, BigDecimal> requerimientos = Map.of(producto, BigDecimal.TEN);

        mockInventarioDisponible(BigDecimal.valueOf(15));
        mockRecepcionesProgramadas(BigDecimal.ZERO);

        List<DetalleCorridaMrp> netos = service.calcularRequerimientosNetos(
                requerimientos, LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(1, netos.size());
        assertEquals(0, netos.get(0).getRequerimientoNeto().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculaRequerimientoNetoPositivoCuandoInventarioInsuficiente() {
        Producto producto = Producto.builder().id(2).build();
        Map<Producto, BigDecimal> requerimientos = Map.of(producto, BigDecimal.TEN);

        mockInventarioDisponible(BigDecimal.valueOf(2));
        mockRecepcionesProgramadas(BigDecimal.ZERO);

        List<DetalleCorridaMrp> netos = service.calcularRequerimientosNetos(
                requerimientos, LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(BigDecimal.valueOf(8), netos.get(0).getRequerimientoNeto());
    }

    @Test
    void descuentaRecepcionesProgramadasDelNeto() {
        Producto producto = Producto.builder().id(3).build();
        Map<Producto, BigDecimal> requerimientos = Map.of(producto, BigDecimal.TEN);

        mockInventarioDisponible(BigDecimal.ONE);
        mockRecepcionesProgramadas(BigDecimal.valueOf(4));

        List<DetalleCorridaMrp> netos = service.calcularRequerimientosNetos(
                requerimientos, LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(BigDecimal.valueOf(5), netos.get(0).getRequerimientoNeto());
        assertEquals(BigDecimal.valueOf(4), netos.get(0).getRecepcionesProgramadas());
    }

    @Test
    void explotaSemiElaboradoYPropagaRequerimientos() {
        Producto productoTerminado = Producto.builder()
                .id(10)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.PRODUCTO_TERMINADO).build())
                .build();
        Producto semiElaborado = Producto.builder()
                .id(20)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.PRODUCTO_SEMI_ELABORADO).build())
                .build();
        Producto insumoFinal = Producto.builder()
                .id(30)
                .categoriaProducto(CategoriaProducto.builder().tipo(TipoCategoria.MATERIA_PRIMA).build())
                .build();

        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .semanaInicio(LocalDate.of(2024, 2, 5))
                .semanaFin(LocalDate.of(2024, 2, 11))
                .detalles(new ArrayList<>())
                .build();
        plan.getDetalles().add(PlanProduccionDetalle.builder()
                .plan(plan)
                .producto(productoTerminado)
                .cantidadPlanificada(BigDecimal.ONE)
                .build());

        FormulaProducto formulaPt = FormulaProducto.builder()
                .producto(productoTerminado)
                .estado(EstadoFormula.APROBADA)
                .detalles(List.of(DetalleFormula.builder()
                        .insumo(semiElaborado)
                        .cantidadNecesaria(BigDecimal.TEN)
                        .build()))
                .build();
        FormulaProducto formulaPs = FormulaProducto.builder()
                .producto(semiElaborado)
                .estado(EstadoFormula.APROBADA)
                .detalles(List.of(DetalleFormula.builder()
                        .insumo(insumoFinal)
                        .cantidadNecesaria(BigDecimal.valueOf(2))
                        .build()))
                .build();

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(productoTerminado.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPt));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(semiElaborado.getId().longValue(), EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPs));

        mockInventarioDisponible(Map.of(
                semiElaborado.getId().longValue(), BigDecimal.valueOf(3),
                insumoFinal.getId().longValue(), BigDecimal.ZERO
        ));
        mockRecepcionesProgramadas(Map.of(semiElaborado.getId().longValue(), BigDecimal.ONE));

        Map<Producto, BigDecimal> brutos = service.calcularRequerimientosBrutos(plan);

        assertEquals(BigDecimal.TEN, brutos.get(semiElaborado));
        assertEquals(BigDecimal.valueOf(12), brutos.get(insumoFinal));

        List<DetalleCorridaMrp> netos = service.calcularRequerimientosNetos(
                brutos, plan.getSemanaInicio(), plan.getSemanaFin());
        Map<Integer, DetalleCorridaMrp> netosPorProducto = netos.stream()
                .collect(java.util.stream.Collectors.toMap(d -> d.getProducto().getId(), d -> d));

        assertEquals(BigDecimal.valueOf(6), netosPorProducto.get(semiElaborado.getId()).getRequerimientoNeto());
        assertEquals(BigDecimal.valueOf(12), netosPorProducto.get(insumoFinal.getId()).getRequerimientoNeto());

        List<com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento> sugerencias = service.generarSugerencias(netos);
        TipoSugerenciaAbastecimiento tipoSugerenciaSemi = sugerencias.stream()
                .filter(s -> s.getDetalleCorrida().getProducto().getId().equals(semiElaborado.getId()))
                .findFirst()
                .map(com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento::getTipo)
                .orElse(null);

        assertEquals(TipoSugerenciaAbastecimiento.FABRICAR, tipoSugerenciaSemi);
    }

    @Test
    void asignaTipoCambioComparandoConCorridaAnterior() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
                .semanaInicio(LocalDate.of(2024, 1, 1))
                .semanaFin(LocalDate.of(2024, 1, 7))
                .build();

        Producto insumoNuevo = Producto.builder().id(1).build();
        Producto insumoAumenta = Producto.builder().id(2).build();
        Producto insumoReduce = Producto.builder().id(3).build();
        Producto insumoIgual = Producto.builder().id(4).build();

        DetalleCorridaMrp detalleAnteriorAumenta = DetalleCorridaMrp.builder()
                .producto(insumoAumenta)
                .requerimientoNeto(BigDecimal.valueOf(5))
                .nivelBom(1)
                .build();
        DetalleCorridaMrp detalleAnteriorReduce = DetalleCorridaMrp.builder()
                .producto(insumoReduce)
                .requerimientoNeto(BigDecimal.valueOf(4))
                .nivelBom(1)
                .build();
        DetalleCorridaMrp detalleAnteriorIgual = DetalleCorridaMrp.builder()
                .producto(insumoIgual)
                .requerimientoNeto(BigDecimal.valueOf(7))
                .nivelBom(1)
                .build();

        CorridaMrp corridaAnterior = CorridaMrp.builder()
                .planProduccionSemanal(plan)
                .estado(EstadoCorridaMrp.COMPLETADA)
                .detalles(new ArrayList<>(List.of(detalleAnteriorAumenta, detalleAnteriorReduce, detalleAnteriorIgual)))
                .build();
        corridaAnterior.getDetalles().forEach(d -> d.setCorrida(corridaAnterior));

        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(
                plan, EstadoCorridaMrp.COMPLETADA)).thenReturn(Optional.of(corridaAnterior));

        List<DetalleCorridaMrp> nuevosDetalles = List.of(
                DetalleCorridaMrp.builder().producto(insumoNuevo).requerimientoNeto(BigDecimal.valueOf(3)).nivelBom(1).build(),
                DetalleCorridaMrp.builder().producto(insumoAumenta).requerimientoNeto(BigDecimal.TEN).nivelBom(1).build(),
                DetalleCorridaMrp.builder().producto(insumoReduce).requerimientoNeto(BigDecimal.valueOf(2)).nivelBom(1).build(),
                DetalleCorridaMrp.builder().producto(insumoIgual).requerimientoNeto(BigDecimal.valueOf(7)).nivelBom(1).build()
        );

        doReturn(Collections.emptyMap()).when(service).calcularRequerimientosBrutos(plan);
        doReturn(nuevosDetalles).when(service).calcularRequerimientosNetos(anyMap(), any(LocalDate.class), any(LocalDate.class));
        doReturn(Collections.emptyList()).when(service).generarSugerencias(any());
        when(corridaMrpRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CorridaMrp resultado = service.ejecutarCorridaSemana(plan);

        Map<Producto, TipoCambioMrp> cambios = resultado.getDetalles().stream()
                .collect(java.util.stream.Collectors.toMap(DetalleCorridaMrp::getProducto, DetalleCorridaMrp::getTipoCambioMrp));

        assertEquals(TipoCambioMrp.NUEVO, cambios.get(insumoNuevo));
        assertEquals(TipoCambioMrp.AUMENTO, cambios.get(insumoAumenta));
        assertEquals(TipoCambioMrp.REDUCCION, cambios.get(insumoReduce));
        assertEquals(TipoCambioMrp.SIN_CAMBIO, cambios.get(insumoIgual));
    }

    @Test
    void calculaProyeccionYCriticidadCritica() {
        CorridaMrp corrida = CorridaMrp.builder()
                .horizonteInicio(LocalDate.of(2024, 1, 1))
                .horizonteFin(LocalDate.of(2024, 1, 7))
                .build();
        Producto producto = Producto.builder()
                .id(1)
                .leadTimeCompraDias(14)
                .build();
        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .corrida(corrida)
                .producto(producto)
                .requerimientoBruto(BigDecimal.valueOf(120))
                .inventarioDisponible(BigDecimal.valueOf(40))
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.valueOf(100))
                .nivelBom(1)
                .build();

        List<com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento> sugerencias = service.generarSugerencias(List.of(detalle));

        assertEquals(1, sugerencias.size());
        com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento sugerencia = sugerencias.get(0);
        assertEquals(BigDecimal.valueOf(120), sugerencia.getConsumoTotalPeriodo());
        assertEquals(BigDecimal.valueOf(120.00).setScale(2), sugerencia.getConsumoSemanalPromedio());
        assertEquals(BigDecimal.valueOf(0.33).setScale(2), sugerencia.getSemanasCobertura());
        assertEquals("CRITICO", sugerencia.getNivelCriticidad());
        assertEquals(Boolean.TRUE, sugerencia.getEsCritico());
        assertEquals(2, sugerencia.getRazonesCriticidad().size());
    }

    @Test
    void calculaCriticidadBajaCuandoCoberturaAlta() {
        CorridaMrp corrida = CorridaMrp.builder()
                .horizonteInicio(LocalDate.of(2024, 1, 1))
                .horizonteFin(LocalDate.of(2024, 1, 28))
                .build();
        Producto producto = Producto.builder()
                .id(2)
                .leadTimeCompraDias(7)
                .build();
        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .corrida(corrida)
                .producto(producto)
                .requerimientoBruto(BigDecimal.valueOf(10))
                .inventarioDisponible(BigDecimal.valueOf(1000))
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.TEN)
                .nivelBom(1)
                .build();

        List<com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento> sugerencias = service.generarSugerencias(List.of(detalle));

        assertEquals(1, sugerencias.size());
        com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento sugerencia = sugerencias.get(0);
        assertEquals("BAJO", sugerencia.getNivelCriticidad());
        assertEquals(Boolean.FALSE, sugerencia.getEsCritico());
        assertEquals(BigDecimal.valueOf(2.50).setScale(2), sugerencia.getConsumoSemanalPromedio());
        assertEquals(BigDecimal.valueOf(400.00).setScale(2), sugerencia.getSemanasCobertura());
        assertNotNull(sugerencia.getRazonesCriticidad());
        assertEquals(0, sugerencia.getRazonesCriticidad().size());
    }

    @Test
    void manejaHorizonteInvalidoSinDividirPorCero() {
        CorridaMrp corrida = CorridaMrp.builder()
                .horizonteInicio(null)
                .horizonteFin(null)
                .build();
        Producto producto = Producto.builder()
                .id(3)
                .leadTimeCompraDias(7)
                .build();
        DetalleCorridaMrp detalle = DetalleCorridaMrp.builder()
                .corrida(corrida)
                .producto(producto)
                .requerimientoBruto(BigDecimal.TEN)
                .inventarioDisponible(BigDecimal.ZERO)
                .recepcionesProgramadas(BigDecimal.ZERO)
                .requerimientoNeto(BigDecimal.TEN)
                .nivelBom(1)
                .build();

        List<com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento> sugerencias = service.generarSugerencias(List.of(detalle));

        assertEquals(1, sugerencias.size());
        com.willyes.clemenintegra.planeacion.model.SugerenciaAbastecimiento sugerencia = sugerencias.get(0);
        assertNotNull(sugerencia.getConsumoTotalPeriodo());
        assertNull(sugerencia.getConsumoSemanalPromedio());
        assertNull(sugerencia.getSemanasCobertura());
        assertEquals("ALTO", sugerencia.getNivelCriticidad());
        assertEquals(Boolean.FALSE, sugerencia.getEsCritico());
        assertNotNull(sugerencia.getRazonesCriticidad());
        assertEquals(0, sugerencia.getRazonesCriticidad().size());
    }

    private void mockInventarioDisponible(BigDecimal cantidad) {
        List<Object[]> resultados = List.<Object[]>of(new Object[]{EstadoLote.DISPONIBLE, cantidad});
        when(loteProductoRepository.sumarPorEstado(anyLong())).thenReturn(resultados);
    }

    private void mockInventarioDisponible(Map<Long, BigDecimal> cantidadesPorProducto) {
        when(loteProductoRepository.sumarPorEstado(anyLong())).thenAnswer(invocation -> {
            Long productoId = invocation.getArgument(0);
            BigDecimal cantidad = cantidadesPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
            return List.<Object[]>of(new Object[]{EstadoLote.DISPONIBLE, cantidad});
        });
    }

    private void mockRecepcionesProgramadas(BigDecimal cantidad) {
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstadoYFechas(
                anyLong(), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(cantidad);
    }

    private void mockRecepcionesProgramadas(Map<Long, BigDecimal> cantidadesPorProducto) {
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstadoYFechas(
                anyLong(), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    Long productoId = invocation.getArgument(0);
                    return cantidadesPorProducto.getOrDefault(productoId, BigDecimal.ZERO);
                });
    }
}
