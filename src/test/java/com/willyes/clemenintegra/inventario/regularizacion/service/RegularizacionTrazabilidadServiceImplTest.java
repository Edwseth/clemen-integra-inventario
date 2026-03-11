package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.RegularizacionTrazabilidadServiceImpl;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.inventario.service.InventoryCatalogResolver;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import com.willyes.clemenintegra.produccion.model.enums.EstadoProduccion;
import com.willyes.clemenintegra.produccion.repository.OrdenProduccionRepository;
import com.willyes.clemenintegra.shared.exception.ApiErrorCode;
import com.willyes.clemenintegra.shared.exception.CustomBusinessException;
import com.willyes.clemenintegra.shared.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegularizacionTrazabilidadServiceImplTest {
    private static final Integer ALMACEN_PRE_BODEGA = 6;

    @Mock OrdenProduccionRepository ordenProduccionRepository;
    @Mock MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock LoteProductoRepository loteProductoRepository;
    @Mock MotivoMovimientoRepository motivoMovimientoRepository;
    @Mock TipoMovimientoDetalleRepository tipoMovimientoDetalleRepository;
    @Mock MovimientoInventarioService movimientoInventarioService;
    @Mock InventoryCatalogResolver inventoryCatalogResolver;
    @Mock FormulaProductoRepository formulaProductoRepository;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadRepository regularizacionRepository;
    @Mock com.willyes.clemenintegra.inventario.regularizacion.repository.RegularizacionTrazabilidadDetalleRepository detalleRepository;
    @InjectMocks RegularizacionTrazabilidadServiceImpl service;

    @Test
    void diferenciaPositiva_debeCrearSalidaAdicionalEmpaque() {
        Usuario u = Usuario.builder().id(1L).build();
        setupRegularizacionLifecycle("idem-pos", 10L);
        setupCatalogos();

        OrdenProduccion op = op(4L, 10, "6000");
        when(ordenProduccionRepository.findById(4L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEmpaque(21, "1")));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                4L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of(consumo(21, 100L, "6000", ALMACEN_PRE_BODEGA)));
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                4L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(10, 901L, 2)));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(4L, new BigDecimal("6030"), "ACTA", "regularizacion positiva", false), "idem-pos", u);

        assertThat(respuesta.movimientos()).hasSize(2);
        assertThat(respuesta.movimientos()).anySatisfy(m -> {
            assertThat(m.tipoMovimiento()).isEqualTo(TipoMovimiento.SALIDA.name());
            assertThat(m.clasificacion()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD.name());
        });
        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService, atLeastOnce()).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getAllValues())
                .extracting(MovimientoInventarioDTO::clasificacionMovimientoInventario)
                .contains(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD,
                        ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT);
    }

    @Test
    void diferenciaNegativa_debeCrearDevolucionEmpaque() {
        Usuario u = Usuario.builder().id(1L).build();
        setupRegularizacionLifecycle("idem-neg", 11L);
        setupCatalogos();

        OrdenProduccion op = op(8L, 10, "30000");
        when(ordenProduccionRepository.findById(8L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEmpaque(21, "1")));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                8L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of(consumo(21, 200L, "30000", ALMACEN_PRE_BODEGA)));
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                8L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(10, 902L, 2)));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(8L, new BigDecimal("29900"), "ACTA", "regularizacion negativa", false), "idem-neg", u);

        assertThat(respuesta.movimientos()).hasSize(2);
        assertThat(respuesta.movimientos())
                .extracting(m -> m.clasificacion())
                .contains(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION.name(),
                        ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT.name());
    }

    @Test
    void sinEmpaquesYFlagPtFalse_debeFallarSinAcciones_yNoGuardarCabecera() {
        Usuario u = Usuario.builder().id(2L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-sin-acciones")).thenReturn(Optional.empty());

        OrdenProduccion op = op(9L, 10, "100");
        when(ordenProduccionRepository.findById(9L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(FormulaProducto.builder().detalles(List.of()).build()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                9L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(9L, new BigDecimal("100"), "ACTA", "sin acciones", false), "idem-sin-acciones", u))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(e -> ((CustomBusinessException) e).getCode())
                .isEqualTo(ApiErrorCode.REGULARIZACION_SIN_ACCIONES);

        verify(regularizacionRepository, never()).save(any());
        verify(movimientoInventarioService, never()).registrarMovimiento(any(), anyString());
    }

    @Test
    void ptSinEntradaBase_debeFallar422() {
        Usuario u = Usuario.builder().id(2L).build();
        when(regularizacionRepository.findByIdempotencyKey("idem-pt")).thenReturn(Optional.empty());

        OrdenProduccion op = op(10L, 10, "6000");
        when(ordenProduccionRepository.findById(10L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEmpaque(21, "1")));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                10L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of(consumo(21, 500L, "6000", ALMACEN_PRE_BODEGA)));
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                10L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(10L, new BigDecimal("6030"), "ACTA", "pt sin base", true), "idem-pt", u))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(e -> ((CustomBusinessException) e).getCode())
                .isEqualTo(ApiErrorCode.REGULARIZACION_PT_SIN_ENTRADA_BASE);
    }

    @Test
    void regularizacionProductoResultadoPt_diferenciaPositiva_usaClasificacionYAlmacenPtDelLote() {
        Usuario u = Usuario.builder().id(7L).build();
        setupRegularizacionLifecycle("idem-pt-pos", 21L);
        setupCatalogos();

        OrdenProduccion op = op(21L, 10, "100", TipoCategoria.PRODUCTO_TERMINADO);
        when(ordenProduccionRepository.findById(21L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(FormulaProducto.builder().detalles(List.of()).build()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                21L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of());
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                21L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(10, 910L, 2)));

        service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(21L, new BigDecimal("105"), "ACTA", "pt pos", false), "idem-pt-pos", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT);
        assertThat(captor.getValue().almacenDestinoId()).isEqualTo(2);
    }

    @Test
    void regularizacionProductoResultadoPt_diferenciaNegativa_usaAlmacenOrigenDelLote() {
        Usuario u = Usuario.builder().id(7L).build();
        setupRegularizacionLifecycle("idem-pt-neg", 22L);
        setupCatalogos();

        OrdenProduccion op = op(22L, 10, "100", TipoCategoria.PRODUCTO_TERMINADO);
        when(ordenProduccionRepository.findById(22L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(FormulaProducto.builder().detalles(List.of()).build()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                22L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of());
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                22L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(10, 911L, 2)));

        service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(22L, new BigDecimal("95"), "ACTA", "pt neg", true), "idem-pt-neg", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().almacenOrigenId()).isEqualTo(2);
        assertThat(captor.getValue().clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PT);
    }

    @Test
    void regularizacionProductoResultadoPs_diferenciaPositiva_usaClasificacionYAlmacenPsDelLote() {
        Usuario u = Usuario.builder().id(7L).build();
        setupRegularizacionLifecycle("idem-ps-pos", 23L);
        setupCatalogos();

        OrdenProduccion op = op(23L, 11, "50", TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        when(ordenProduccionRepository.findById(23L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(11L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(FormulaProducto.builder().detalles(List.of()).build()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                23L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of());
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                23L, 11L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(11, 912L, 7)));

        service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(23L, new BigDecimal("55"), "ACTA", "ps pos", false), "idem-ps-pos", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().almacenDestinoId()).isEqualTo(7);
        assertThat(captor.getValue().clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PS);
    }

    @Test
    void regularizacionProductoResultadoPs_diferenciaNegativa_usaClasificacionPsYAlmacenOrigenDelLote() {
        Usuario u = Usuario.builder().id(7L).build();
        setupRegularizacionLifecycle("idem-ps-neg", 24L);
        setupCatalogos();

        OrdenProduccion op = op(24L, 11, "50", TipoCategoria.PRODUCTO_SEMI_ELABORADO);
        when(ordenProduccionRepository.findById(24L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(11L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(FormulaProducto.builder().detalles(List.of()).build()));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                24L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of());
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                24L, 11L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(11, 913L, 7)));

        service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(24L, new BigDecimal("45"), "ACTA", "ps neg", true), "idem-ps-neg", u);

        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().almacenOrigenId()).isEqualTo(7);
        assertThat(captor.getValue().clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD_PS);
    }


    @Test
    void regularizacionPostCierreTotal_recalculaCostoUnitarioLoteProducido() {
        Usuario u = Usuario.builder().id(1L).build();
        setupRegularizacionLifecycle("idem-cierre", 15L);
        setupCatalogos();

        OrdenProduccion op = op(12L, 10, "100");
        op.setEstado(EstadoProduccion.FINALIZADA);
        when(ordenProduccionRepository.findById(12L)).thenReturn(Optional.of(op));
        when(formulaProductoRepository.findByProductoIdAndEstadoAndActivoTrue(10L, EstadoFormula.APROBADA))
                .thenReturn(Optional.of(formulaEmpaque(21, "1")));
        when(movimientoInventarioRepository.findByOrdenProduccionIdAndClasificacionAndTipoMovimientoOrderByFechaIngresoAscIdAsc(
                12L, ClasificacionMovimientoInventario.SALIDA_PRODUCCION, TipoMovimiento.SALIDA
        )).thenReturn(List.of(consumo(21, 300L, "100", ALMACEN_PRE_BODEGA)));
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndProductoIdAndTipoMovimientoOrderByIdAsc(
                12L, 10L, TipoMovimiento.ENTRADA
        )).thenReturn(Optional.of(entradaBase(10, 903L, 2)));
        when(movimientoInventarioRepository.sumarCostoMaterialRealOp(12L)).thenReturn(new BigDecimal("500.000000"));

        LoteProducto lotePt = new LoteProducto();
        lotePt.setId(901L);
        lotePt.setCostoUnitarioMaterial(BigDecimal.ZERO);
        when(loteProductoRepository.findByOrdenProduccionIdAndProductoId(12L, 10L)).thenReturn(Optional.of(lotePt));

        service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(12L, new BigDecimal("80"), "ACTA", "post cierre", false), "idem-cierre", u);

        verify(loteProductoRepository).save(argThat(lote ->
                lote.getId().equals(901L)
                        && lote.getCostoTotalMaterialIngresado().compareTo(new BigDecimal("500.000000")) == 0
                        && lote.getCostoUnitarioMaterial().compareTo(new BigDecimal("6.250000")) == 0));
    }

    @Test
    void idempotenciaConDetalles_retornaExistente() {
        Usuario u = Usuario.builder().id(3L).build();
        RegularizacionTrazabilidad existente = RegularizacionTrazabilidad.builder()
                .id(50L)
                .ordenProduccionId(98L)
                .cantidadProgramada(new BigDecimal("30"))
                .cantidadReal(new BigDecimal("28"))
                .diferencia(new BigDecimal("-2"))
                .idempotencyKey("idem-dup")
                .usuario(u)
                .fechaIngreso(LocalDateTime.now())
                .build();
        when(regularizacionRepository.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existente));

        RegularizacionTrazabilidadDetalle d = RegularizacionTrazabilidadDetalle.builder()
                .regularizacion(existente)
                .productoId(21L)
                .loteId(100L)
                .cantidad(new BigDecimal("2"))
                .tipo(TipoMovimiento.ENTRADA.name())
                .movimiento(MovimientoInventario.builder().id(900L).build())
                .build();
        when(detalleRepository.findByRegularizacionIdOrderByIdAsc(50L)).thenReturn(List.of(d));
        when(movimientoInventarioRepository.findById(900L)).thenReturn(Optional.of(MovimientoInventario.builder()
                .id(900L)
                .clasificacion(ClasificacionMovimientoInventario.DEVOLUCION_DESDE_PRODUCCION)
                .build()));

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(98L, new BigDecimal("28"), "ACTA", "regularizacion op 98", false), "idem-dup", u);

        assertThat(respuesta.movimientos()).hasSize(1);
    }

    @Test
    void idempotenciaSinDetalles_debeFallar409() {
        Usuario u = Usuario.builder().id(3L).build();
        RegularizacionTrazabilidad existente = RegularizacionTrazabilidad.builder()
                .id(70L)
                .ordenProduccionId(98L)
                .cantidadProgramada(new BigDecimal("30"))
                .cantidadReal(new BigDecimal("28"))
                .diferencia(new BigDecimal("-2"))
                .idempotencyKey("idem-bad")
                .usuario(u)
                .fechaIngreso(LocalDateTime.now())
                .build();
        when(regularizacionRepository.findByIdempotencyKey("idem-bad")).thenReturn(Optional.of(existente));
        when(detalleRepository.findByRegularizacionIdOrderByIdAsc(70L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(98L, new BigDecimal("28"), "ACTA", "regularizacion op 98", false), "idem-bad", u))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(e -> ((CustomBusinessException) e).getCode())
                .isEqualTo(ApiErrorCode.REGULARIZACION_IDEMPOTENTE_INCONSISTENTE);
    }

    private void setupCatalogos() {
        when(inventoryCatalogResolver.getAlmacenPtId()).thenReturn(2L);
        when(inventoryCatalogResolver.getAlmacenOrigenProductoSemiElaboradoId()).thenReturn(7L);
        when(tipoMovimientoDetalleRepository.findByDescripcion(anyString()))
                .thenReturn(Optional.of(TipoMovimientoDetalle.builder().id(1L).descripcion("x").build()));
        when(motivoMovimientoRepository.findByMotivo(any()))
                .thenReturn(Optional.of(MotivoMovimiento.builder().id(1L).build()));
        when(movimientoInventarioService.registrarMovimiento(any(), anyString()))
                .thenReturn(MovimientoInventarioResponseDTO.builder().id(99L).build());
    }

    private void setupRegularizacionLifecycle(String idem, Long id) {
        AtomicReference<RegularizacionTrazabilidad> ref = new AtomicReference<>();
        when(regularizacionRepository.findByIdempotencyKey(idem)).thenAnswer(i -> Optional.ofNullable(ref.get()));
        when(regularizacionRepository.save(any(RegularizacionTrazabilidad.class))).thenAnswer(i -> {
            RegularizacionTrazabilidad reg = i.getArgument(0, RegularizacionTrazabilidad.class);
            reg.setId(id);
            ref.set(reg);
            return reg;
        });
    }

    private OrdenProduccion op(Long id, int productoId, String programada) {
        return op(id, productoId, programada, TipoCategoria.PRODUCTO_TERMINADO);
    }

    private OrdenProduccion op(Long id, int productoId, String programada, TipoCategoria tipoCategoria) {
        Producto pt = new Producto();
        pt.setId(productoId);
        CategoriaProducto categoriaProducto = new CategoriaProducto();
        categoriaProducto.setTipo(tipoCategoria);
        pt.setCategoriaProducto(categoriaProducto);
        return OrdenProduccion.builder().id(id).producto(pt).cantidadProgramada(new BigDecimal(programada)).build();
    }

    private FormulaProducto formulaEmpaque(int productoId, String coef) {
        CategoriaProducto catEmp = new CategoriaProducto();
        catEmp.setId(2L);
        Producto insumo = new Producto();
        insumo.setId(productoId);
        insumo.setCategoriaProducto(catEmp);
        DetalleFormula det = DetalleFormula.builder().insumo(insumo).cantidadNecesaria(new BigDecimal(coef)).build();
        return FormulaProducto.builder().detalles(List.of(det)).build();
    }

    private MovimientoInventario consumo(int productoId, long loteId, String cantidad, Integer almacenOrigen) {
        CategoriaProducto c = new CategoriaProducto();
        c.setId(2L);
        Producto p = new Producto();
        p.setId(productoId);
        p.setCategoriaProducto(c);
        LoteProducto l = new LoteProducto();
        l.setId(loteId);
        Almacen almacen = new Almacen();
        almacen.setId(almacenOrigen);
        return MovimientoInventario.builder()
                .id(loteId)
                .producto(p)
                .lote(l)
                .cantidad(new BigDecimal(cantidad))
                .almacenOrigen(almacen)
                .fechaIngreso(LocalDateTime.now())
                .build();
    }

    private MovimientoInventario entradaBase(int productoId, long loteId, int almacenLote) {
        Producto p = new Producto();
        p.setId(productoId);
        LoteProducto lote = new LoteProducto();
        lote.setId(loteId);
        Almacen almacen = new Almacen();
        almacen.setId(almacenLote);
        lote.setAlmacen(almacen);
        return MovimientoInventario.builder()
                .id(loteId)
                .producto(p)
                .lote(lote)
                .cantidad(BigDecimal.ONE)
                .tipoMovimiento(TipoMovimiento.ENTRADA)
                .clasificacion(ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO)
                .fechaIngreso(LocalDateTime.now())
                .build();
    }
}
