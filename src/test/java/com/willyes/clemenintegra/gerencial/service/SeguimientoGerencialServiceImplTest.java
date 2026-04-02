package com.willyes.clemenintegra.gerencial.service;

import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.gerencial.dto.SeguimientoGerencialResponseDTO;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.OrdenCompraDetalleRepository;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionDetalle;
import com.willyes.clemenintegra.planeacion.model.PlanProduccionSemanal;
import com.willyes.clemenintegra.planeacion.model.enums.EstadoPlanProduccion;
import com.willyes.clemenintegra.planeacion.repository.CorridaMrpRepository;
import com.willyes.clemenintegra.planeacion.repository.PlanProduccionSemanalRepository;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.repository.EtapaProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoGerencialServiceImplTest {

    @Mock
    private PlanProduccionSemanalRepository planProduccionSemanalRepository;
    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private FormulaProductoRepository formulaProductoRepository;
    @Mock
    private CorridaMrpRepository corridaMrpRepository;
    @Mock
    private OrdenCompraDetalleRepository ordenCompraDetalleRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private EtapaProduccionRepository etapaProduccionRepository;

    @InjectMocks
    private SeguimientoGerencialServiceImpl service;

    @Test
    void debeRetornarItemsUnoAUnoYSummaryConsistente() {
        PlanProduccionSemanal plan = planConDosDetalles(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(5));

        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of());
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of());
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO response = service.obtenerSeguimiento(1L);

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertEquals(2, response.getSummary().getTotalItems());
    }

    @Test
    void sinOpEnVentanaNormalDebeSerNoIniciado() {
        PlanProduccionSemanal plan = planConUnDetalle(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(6));
        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of());
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of(formulaAprobada(plan.getDetalles().get(0).getProducto().getId().longValue())));
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO.ItemDTO item = service.obtenerSeguimiento(1L).getItems().getFirst();

        assertEquals("NO_INICIADO", item.getEstadoGerencial());
    }

    @Test
    void sinOpEnVentanaCriticaDebeSerEnRiesgo() {
        PlanProduccionSemanal plan = planConUnDetalle(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(1));
        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of());
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of(formulaAprobada(plan.getDetalles().get(0).getProducto().getId().longValue())));
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO.ItemDTO item = service.obtenerSeguimiento(1L).getItems().getFirst();

        assertEquals("EN_RIESGO", item.getEstadoGerencial());
    }

    @Test
    void sinFormulaDebeGanarBloqueoYEtapaBomFormulaAunSinOp() {
        PlanProduccionSemanal plan = planConUnDetalle(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(1));
        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of());
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of());
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO.ItemDTO item = service.obtenerSeguimiento(1L).getItems().getFirst();

        assertEquals("BOM_FORMULA", item.getEtapaActual());
        assertEquals("BLOQUEADO", item.getEstadoGerencial());
        assertEquals("SIN_FORMULA_APROBADA", item.getBloqueoPrincipal().getCodigo());
    }

    @Test
    void etapaNoDebeSerPlaneacionSoloPorAusenciaDeOp() {
        PlanProduccionSemanal plan = planConUnDetalle(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(6));
        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));
        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of());
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of(formulaAprobada(plan.getDetalles().get(0).getProducto().getId().longValue())));
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO.ItemDTO item = service.obtenerSeguimiento(1L).getItems().getFirst();

        assertNotEquals("PLANEACION", item.getEtapaActual());
    }

    @Test
    void summaryDebeAgregarConteosDeEstados() {
        PlanProduccionSemanal plan = planConDosDetalles(EstadoPlanProduccion.CONFIRMADO, LocalDate.now().plusDays(6));
        when(planProduccionSemanalRepository.findWithDetallesById(1L)).thenReturn(Optional.of(plan));

        OrdenProduccion op = OrdenProduccion.builder()
                .id(9L)
                .planProduccionDetalle(plan.getDetalles().get(1))
                .cantidadProducidaAcumulada(BigDecimal.TEN)
                .cantidadProgramada(BigDecimal.TEN)
                .estado(com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion.EN_PROCESO)
                .build();

        when(ordenProduccionRepository.findByPlanProduccionDetalleIdIn(anySet())).thenReturn(List.of(op));
        when(formulaProductoRepository.findByProductoIdInAndEstadoAndActivoTrue(anyCollection(), any())).thenReturn(List.of(
                formulaAprobada(plan.getDetalles().get(0).getProducto().getId().longValue()),
                formulaAprobada(plan.getDetalles().get(1).getProducto().getId().longValue())
        ));
        when(corridaMrpRepository.findTopByPlanProduccionSemanalAndEstadoOrderByFechaEjecucionDesc(any(), any())).thenReturn(Optional.empty());
        when(ordenCompraDetalleRepository.sumarCantidadPendientePorProductoYEstados(anyLong(), any())).thenReturn(BigDecimal.ZERO);

        SeguimientoGerencialResponseDTO response = service.obtenerSeguimiento(1L);

        assertEquals(2, response.getSummary().getTotalItems());
        assertEquals(1, response.getSummary().getItemsNoIniciados());
        assertEquals(1, response.getSummary().getItemsEnProceso());
    }

    private PlanProduccionSemanal planConUnDetalle(EstadoPlanProduccion estado, LocalDate semanaFin) {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .semanaInicio(LocalDate.now())
                .semanaFin(semanaFin)
                .estado(estado)
                .build();
        PlanProduccionDetalle detalle = detalle(11L, 101, plan, BigDecimal.TEN);
        plan.setDetalles(List.of(detalle));
        return plan;
    }

    private PlanProduccionSemanal planConDosDetalles(EstadoPlanProduccion estado, LocalDate semanaFin) {
        PlanProduccionSemanal plan = PlanProduccionSemanal.builder()
                .id(1L)
                .semanaInicio(LocalDate.now())
                .semanaFin(semanaFin)
                .estado(estado)
                .build();
        PlanProduccionDetalle d1 = detalle(11L, 101, plan, BigDecimal.TEN);
        PlanProduccionDetalle d2 = detalle(12L, 102, plan, BigDecimal.TEN);
        plan.setDetalles(List.of(d1, d2));
        return plan;
    }

    private PlanProduccionDetalle detalle(Long id, Integer productoId, PlanProduccionSemanal plan, BigDecimal cantidadPlanificada) {
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setCodigoSku("SKU-" + productoId);
        producto.setNombre("Producto " + productoId);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("Kilogramo");
        unidad.setSimbolo("kg");

        return PlanProduccionDetalle.builder()
                .id(id)
                .plan(plan)
                .producto(producto)
                .unidadMedida(unidad)
                .cantidadPlanificada(cantidadPlanificada)
                .build();
    }

    private com.willyes.clemenintegra.bom.model.FormulaProducto formulaAprobada(Long productoId) {
        Producto producto = new Producto();
        producto.setId(productoId.intValue());
        return com.willyes.clemenintegra.bom.model.FormulaProducto.builder()
                .id(productoId + 500)
                .producto(producto)
                .estado(com.willyes.clemenintegra.bom.model.enums.EstadoFormula.APROBADA)
                .versionMajor(1)
                .versionMinor(0)
                .activo(true)
                .build();
    }
}
