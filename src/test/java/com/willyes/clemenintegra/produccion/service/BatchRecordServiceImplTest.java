package com.willyes.clemenintegra.produccion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.calidad.model.EvaluacionCalidad;
import com.willyes.clemenintegra.calidad.model.RetencionLote;
import com.willyes.clemenintegra.calidad.model.enums.EstadoRetencion;
import com.willyes.clemenintegra.calidad.repository.EvaluacionCalidadRepository;
import com.willyes.clemenintegra.calidad.repository.RetencionLoteRepository;
import com.willyes.clemenintegra.inventario.model.LoteProducto;
import com.willyes.clemenintegra.inventario.model.MovimientoInventario;
import com.willyes.clemenintegra.inventario.model.Producto;
import com.willyes.clemenintegra.inventario.model.CategoriaProducto;
import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.EstadoLote;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.LoteProductoRepository;
import com.willyes.clemenintegra.inventario.repository.MovimientoInventarioRepository;
import com.willyes.clemenintegra.inventario.repository.ReservaLoteRepository;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDTO;
import com.willyes.clemenintegra.produccion.dto.BatchRecordDecisionRequestDTO;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoBatchRecord;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.CierreProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ControlEmpaqueLoteRepository;
import com.willyes.clemenintegra.produccion.repository.ControlProcesoProduccionRepository;
import com.willyes.clemenintegra.produccion.repository.ObservacionProcesoRepository;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchRecordServiceImplTest {

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;
    @Mock
    private FormulaProductoRepository formulaProductoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock
    private ReservaLoteRepository reservaLoteRepository;
    @Mock
    private CierreProduccionRepository cierreProduccionRepository;
    @Mock
    private LoteProductoRepository loteProductoRepository;
    @Mock
    private EvaluacionCalidadRepository evaluacionCalidadRepository;
    @Mock
    private RetencionLoteRepository retencionLoteRepository;
    @Mock
    private ControlProcesoProduccionRepository controlProcesoProduccionRepository;
    @Mock
    private ControlEmpaqueLoteRepository controlEmpaqueLoteRepository;
    @Mock
    private ObservacionProcesoRepository observacionProcesoRepository;

    private BatchRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BatchRecordServiceImpl(
                ordenProduccionRepository,
                formulaProductoRepository,
                movimientoInventarioRepository,
                reservaLoteRepository,
                cierreProduccionRepository,
                loteProductoRepository,
                evaluacionCalidadRepository,
                retencionLoteRepository,
                controlProcesoProduccionRepository,
                controlEmpaqueLoteRepository,
                observacionProcesoRepository
        );
    }

    @Test
    @DisplayName("buildByOrdenProduccion retorna fórmula y listas vacías cuando no hay movimientos")
    void buildByOrdenProduccionSinMovimientos() {
        OrdenProduccion orden = buildOrdenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        FormulaProducto formula = buildFormula(orden.getProducto());
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result).isNotNull();
        assertThat(result.op).isNotNull();
        assertThat(result.formula).isNotNull();
        assertThat(result.formula.detalles).hasSize(1);
        assertThat(result.consumos).isEmpty();
        assertThat(result.reservas).isEmpty();
        assertThat(result.cierres).isEmpty();
        assertThat(result.calidad.evaluaciones).isEmpty();
    }

    @Test
    @DisplayName("mapFormula mantiene el detalle cuando el insumo no es PS")
    void mapFormulaSinProductoSemielaborado() {
        OrdenProduccion orden = buildOrdenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Producto mp = productoConCategoria(20, "MP-1", "Materia prima", TipoCategoria.MATERIA_PRIMA);
        FormulaProducto formula = buildFormulaConDetalle(orden.getProducto(), mp, BigDecimal.TEN);
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result.formula.detalles).hasSize(1);
        assertThat(result.formula.detalles.get(0).codigoSku).isEqualTo("MP-1");
        assertThat(result.formula.detalles.get(0).cantidadNecesaria)
                .isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("mapFormula expande PS y calcula cantidades teóricas según la OP PT")
    void mapFormulaConProductoSemielaborado() {
        OrdenProduccion orden = buildOrdenProduccion();
        orden.setCantidadProgramada(new BigDecimal("3"));
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Producto ps = productoConCategoria(30, "PS-1", "Semi", TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        FormulaProducto formulaPt = buildFormulaConDetalle(orden.getProducto(), ps, new BigDecimal("2"));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPt));

        Producto mp1 = productoConCategoria(40, "MP-1", "Materia 1", TipoCategoria.MATERIA_PRIMA);
        Producto mp2 = productoConCategoria(41, "MP-2", "Materia 2", TipoCategoria.MATERIA_PRIMA);
        FormulaProducto formulaPs = new FormulaProducto();
        formulaPs.setProducto(ps);
        formulaPs.setDetalles(List.of(
                detalleFormula(mp1, new BigDecimal("0.5")),
                detalleFormula(mp2, new BigDecimal("1.25"))
        ));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(30L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPs));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result.formula.detalles)
                .extracting(d -> d.codigoSku)
                .containsExactlyInAnyOrder("MP-1", "MP-2");
        assertThat(result.formula.detalles)
                .extracting(d -> d.cantidadNecesaria)
                .containsExactlyInAnyOrder(
                        new BigDecimal("3.0"), // 0.5 * 2 * 3
                        new BigDecimal("7.50")  // 1.25 * 2 * 3
                );
    }

    @Test
    @DisplayName("mapFormula escala componentes directos del PT por cantidad programada cuando no hay PS")
    void mapFormulaEscalaComponentesDirectosSinPs() {
        OrdenProduccion orden = buildOrdenProduccion();
        orden.setCantidadProgramada(new BigDecimal("100"));
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Producto capsula = productoConCategoria(21, "ME-CAP", "Cápsula", TipoCategoria.MATERIA_PRIMA);
        Producto etiqueta = productoConCategoria(22, "ME-ETI", "Etiqueta", TipoCategoria.MATERIA_PRIMA);
        Producto linner = productoConCategoria(23, "ME-LIN", "Linner", TipoCategoria.MATERIA_PRIMA);

        FormulaProducto formula = buildFormulaConDetalles(orden.getProducto(), List.of(
                detalleFormula(capsula, BigDecimal.ONE),
                detalleFormula(etiqueta, BigDecimal.ONE),
                detalleFormula(linner, BigDecimal.ONE)
        ));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formula));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        Map<String, BigDecimal> cantidades = result.formula.detalles.stream()
                .collect(Collectors.toMap(d -> d.codigoSku, d -> d.cantidadNecesaria));

        assertThat(cantidades)
                .containsEntry("ME-CAP", new BigDecimal("100"))
                .containsEntry("ME-ETI", new BigDecimal("100"))
                .containsEntry("ME-LIN", new BigDecimal("100"));
    }

    @Test
    @DisplayName("mapFormula escala insumos directos y mantiene expansión de PS sin duplicar cantidades")
    void mapFormulaEscalaComponentesConPs() {
        OrdenProduccion orden = buildOrdenProduccion();
        orden.setCantidadProgramada(new BigDecimal("30"));
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Producto envase = productoConCategoria(24, "ME-ENV", "Envase", TipoCategoria.MATERIA_PRIMA);
        Producto etiqueta = productoConCategoria(25, "ME-ETQ", "Etiqueta", TipoCategoria.MATERIA_PRIMA);
        Producto linner = productoConCategoria(26, "ME-LIN", "Linner", TipoCategoria.MATERIA_PRIMA);
        Producto tapa = productoConCategoria(27, "ME-TAP", "Tapa", TipoCategoria.MATERIA_PRIMA);
        Producto capsula = productoConCategoria(28, "ME-CAP", "Cápsula", TipoCategoria.MATERIA_PRIMA);
        Producto ps = productoConCategoria(30, "PS-1", "Semi", TipoCategoria.PRODUCTO_SEMI_ELABORADO);

        FormulaProducto formulaPt = buildFormulaConDetalles(orden.getProducto(), List.of(
                detalleFormula(envase, BigDecimal.ONE),
                detalleFormula(etiqueta, BigDecimal.ONE),
                detalleFormula(linner, BigDecimal.ONE),
                detalleFormula(tapa, BigDecimal.ONE),
                detalleFormula(capsula, BigDecimal.ONE),
                detalleFormula(ps, BigDecimal.ONE)
        ));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPt));

        Producto uva = productoConCategoria(40, "MP-UVA", "UVA", TipoCategoria.MATERIA_PRIMA);
        Producto maltodextrina = productoConCategoria(41, "MP-MAL", "Maltodextrina", TipoCategoria.MATERIA_PRIMA);
        FormulaProducto formulaPs = buildFormulaConDetalles(ps, List.of(
                detalleFormula(uva, new BigDecimal("0.5")),
                detalleFormula(maltodextrina, new BigDecimal("0.25"))
        ));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(30L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPs));

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        Map<String, BigDecimal> cantidades = result.formula.detalles.stream()
                .collect(Collectors.toMap(d -> d.codigoSku, d -> d.cantidadNecesaria));

        assertThat(cantidades)
                .containsEntry("ME-ENV", new BigDecimal("30"))
                .containsEntry("ME-ETQ", new BigDecimal("30"))
                .containsEntry("ME-LIN", new BigDecimal("30"))
                .containsEntry("ME-TAP", new BigDecimal("30"))
                .containsEntry("ME-CAP", new BigDecimal("30"))
                .containsEntry("MP-UVA", new BigDecimal("15.0"))
                .containsEntry("MP-MAL", new BigDecimal("7.50"));
    }

    @Test
    @DisplayName("mapFormula mantiene PS cuando no tiene fórmula")
    void mapFormulaPsSinFormulaDisponible() {
        OrdenProduccion orden = buildOrdenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Producto ps = productoConCategoria(30, "PS-1", "Semi", TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        FormulaProducto formulaPt = buildFormulaConDetalle(orden.getProducto(), ps, BigDecimal.ONE);
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaPt));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(30L, EstadoFormula.APROBADA))
                .thenReturn(Optional.empty());
        when(formulaProductoRepository.findByProductoId(30L)).thenReturn(Optional.empty());

        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result.formula.detalles).hasSize(1);
        assertThat(result.formula.detalles.get(0).codigoSku).isEqualTo("PS-1");
    }

    @Test
    @DisplayName("buildByOrdenProduccion mapea consumos, lote PT y calidad")
    void buildByOrdenProduccionConMovimientoYLotePT() {
        OrdenProduccion orden = buildOrdenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.empty());
        when(formulaProductoRepository.findByProductoId(10L)).thenReturn(Optional.of(buildFormula(orden.getProducto())));

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        movimiento.setProducto(orden.getProducto());
        LoteProducto loteMp = new LoteProducto();
        loteMp.setId(5L);
        loteMp.setCodigoLote("L-MP-1");
        movimiento.setLote(loteMp);
        movimiento.setCantidad(BigDecimal.ONE);
        movimiento.setFechaIngreso(LocalDateTime.now());
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(movimiento)));

        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Usuario liberador = new Usuario();
        liberador.setNombreCompleto("Jefe Calidad");
        LoteProducto lotePt = new LoteProducto();
        lotePt.setId(20L);
        lotePt.setCodigoLote("L-PT-1");
        lotePt.setEstado(EstadoLote.LIBERADO);
        lotePt.setFechaLiberacion(LocalDateTime.now());
        lotePt.setUsuarioLiberador(liberador);
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.of(lotePt));

        EvaluacionCalidad evaluacion = new EvaluacionCalidad();
        evaluacion.setId(30L);
        evaluacion.setResultado(com.willyes.clemenintegra.calidad.model.enums.ResultadoEvaluacion.CONFORME);
        evaluacion.setUsuarioEvaluador(liberador);
        when(evaluacionCalidadRepository.findByLoteProductoId(20L)).thenReturn(List.of(evaluacion));

        RetencionLote retencion = new RetencionLote();
        retencion.setEstado(EstadoRetencion.RETENIDO);
        retencion.setAprobadoPor(liberador);
        when(retencionLoteRepository.findByLote_IdAndEstado(20L, EstadoRetencion.RETENIDO))
                .thenReturn(List.of(retencion));
        when(retencionLoteRepository.findByLote_IdAndEstado(20L, EstadoRetencion.LIBERADO))
                .thenReturn(Collections.emptyList());

        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result.consumos).hasSize(1);
        assertThat(result.consumos.get(0).codigoLote).isEqualTo("L-MP-1");
        assertThat(result.loteProductoTerminado.codigoLote).isEqualTo("L-PT-1");
        assertThat(result.loteProductoTerminado.usuarioLiberador).isEqualTo("Jefe Calidad");
        assertThat(result.calidad.evaluaciones).isNotNull();
        assertThat(result.calidad.retenciones).isNotNull();
    }

    @Test
    @DisplayName("mapConsumos ignora movimientos clasificados como producción que no sean salidas")
    void buildByOrdenProduccionIgnoraTiposNoSalida() {
        OrdenProduccion orden = buildOrdenProduccion();
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(buildFormula(orden.getProducto())));

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TipoMovimiento.TRANSFERENCIA);
        movimiento.setClasificacion(ClasificacionMovimientoInventario.SALIDA_PRODUCCION);
        movimiento.setProducto(orden.getProducto());
        movimiento.setCantidad(BigDecimal.ONE);
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacion(
                1L,
                ClasificacionMovimientoInventario.SALIDA_PRODUCCION,
                Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(movimiento)));

        when(reservaLoteRepository.findBySolicitudMovimientoDetalle_SolicitudMovimiento_OrdenProduccionId(1L))
                .thenReturn(Collections.emptyList());
        when(cierreProduccionRepository.findByOrdenProduccionId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(1L, 10L))
                .thenReturn(Optional.empty());
        when(controlProcesoProduccionRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(controlEmpaqueLoteRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());
        when(observacionProcesoRepository.findByOrdenProduccionId(1L)).thenReturn(Collections.emptyList());

        BatchRecordDTO result = service.buildByOrdenProduccion(1L);

        assertThat(result.consumos).isEmpty();
    }

    @Test
    @DisplayName("buildByOrdenProduccion lanza excepción cuando la OP no existe")
    void buildByOrdenProduccionOrdenInexistente() {
        when(ordenProduccionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buildByOrdenProduccion(99L))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.RECURSO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("decidirBatchRecord establece aprobado y auditoría")
    void decidirBatchRecordAprobado() {
        OrdenProduccion orden = buildOrdenProduccion();
        orden.setBatchRecordEstado(EstadoBatchRecord.BORRADOR);
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenProduccionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = new Usuario();
        usuario.setId(50L);
        usuario.setNombreCompleto("Jefe Calidad");
        Authentication auth = buildAuth(usuario);

        BatchRecordDecisionRequestDTO request = new BatchRecordDecisionRequestDTO();
        request.setDecision(EstadoBatchRecord.APROBADO);
        request.setObservacionesCalidad("Revisión conforme");

        service.decidirBatchRecord(1L, request, auth);

        assertThat(orden.getBatchRecordEstado()).isEqualTo(EstadoBatchRecord.APROBADO);
        assertThat(orden.getBatchRecordRevisadoPor()).isEqualTo(usuario);
        assertThat(orden.getBatchRecordFechaRevision()).isNotNull();
        assertThat(orden.getBatchRecordObservacionesCalidad()).isEqualTo("Revisión conforme");
        verify(ordenProduccionRepository).save(orden);
    }

    @Test
    @DisplayName("decidirBatchRecord rechaza sin observaciones")
    void decidirBatchRecordRechazadoSinObservaciones() {
        OrdenProduccion orden = buildOrdenProduccion();
        orden.setBatchRecordEstado(EstadoBatchRecord.EN_REVISION_CALIDAD);
        when(ordenProduccionRepository.findById(1L)).thenReturn(Optional.of(orden));

        Usuario usuario = new Usuario();
        usuario.setId(51L);
        usuario.setNombreCompleto("Inspector Calidad");
        Authentication auth = buildAuth(usuario);

        BatchRecordDecisionRequestDTO request = new BatchRecordDecisionRequestDTO();
        request.setDecision(EstadoBatchRecord.RECHAZADO);

        assertThatThrownBy(() -> service.decidirBatchRecord(1L, request, auth))
                .isInstanceOf(CustomBusinessException.class)
                .hasFieldOrPropertyWithValue("code", ApiErrorCode.SOLICITUD_INVALIDA);
        verify(ordenProduccionRepository, never()).save(any());
    }

    private OrdenProduccion buildOrdenProduccion() {
        Producto producto = new Producto();
        producto.setId(10);
        producto.setCodigoSku("SKU-10");
        producto.setNombre("Producto Demo");
        UnidadMedida unidadMedida = new UnidadMedida();
        unidadMedida.setNombre("UND");
        OrdenProduccion orden = new OrdenProduccion();
        orden.setId(1L);
        orden.setCodigoOrden("OP-1");
        orden.setProducto(producto);
        orden.setUnidadMedida(unidadMedida);
        orden.setCantidadProgramada(BigDecimal.TEN);
        orden.setCantidadProducida(BigDecimal.ZERO);
        orden.setCantidadProducidaAcumulada(BigDecimal.ZERO);
        orden.setFechaInicio(LocalDateTime.now());
        orden.setEstado(EstadoProduccion.EN_PROCESO);
        return orden;
    }

    private Authentication buildAuth(Usuario usuario) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(new CustomUserDetails(usuario), null,
                "ROL_JEFE_CALIDAD");
        token.setAuthenticated(true);
        return token;
    }

    private Producto productoConCategoria(Integer id, String sku, String nombre, TipoCategoria tipoCategoria) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigoSku(sku);
        producto.setNombre(nombre);
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setTipo(tipoCategoria);
        producto.setCategoriaProducto(categoria);
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("UND");
        producto.setUnidadMedida(unidad);
        return producto;
    }

    private FormulaProducto buildFormulaConDetalle(Producto producto, Producto insumo, BigDecimal cantidad) {
        return buildFormulaConDetalles(producto, List.of(detalleFormula(insumo, cantidad)));
    }

    private FormulaProducto buildFormulaConDetalles(Producto producto, List<DetalleFormula> detalles) {
        FormulaProducto formula = new FormulaProducto();
        formula.setProducto(producto);
        formula.setVersion("v1");
        formula.setEstado(EstadoFormula.APROBADA);
        formula.setDetalles(detalles);
        return formula;
    }

    private DetalleFormula detalleFormula(Producto insumo, BigDecimal cantidad) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("UND");
        return DetalleFormula.builder()
                .cantidadNecesaria(cantidad)
                .insumo(insumo)
                .unidadMedida(unidad)
                .obligatorio(true)
                .build();
    }

    private FormulaProducto buildFormula(Producto producto) {
        FormulaProducto formula = new FormulaProducto();
        formula.setProducto(producto);
        formula.setVersion("v1");
        formula.setEstado(EstadoFormula.APROBADA);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("KG");
        DetalleFormula detalle = DetalleFormula.builder()
                .cantidadNecesaria(BigDecimal.ONE)
                .insumo(producto)
                .unidadMedida(unidad)
                .obligatorio(true)
                .build();
        formula.setDetalles(List.of(detalle));
        return formula;
    }
}
