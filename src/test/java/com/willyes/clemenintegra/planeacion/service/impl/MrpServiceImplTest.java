package com.willyes.clemenintegra.planeacion.service.impl;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.planeacion.model.CorridaMrp;
import com.willyes.clemenintegra.planeacion.model.DetalleCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoCorridaMrp;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.model.enums.TipoCambioMrp;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void asignaTipoCambioComparandoConCorridaAnterior() {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .estado(EstadoPlanProduccion.CONFIRMADO)
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
        doReturn(nuevosDetalles).when(service).calcularRequerimientosNetos(anyMap());
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
}
