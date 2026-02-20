package com.willyes.clemenintegra.inventario.regularizacion.service;

import com.willyes.clemenintegra.bom.model.DetalleFormula;
import com.willyes.clemenintegra.bom.model.FormulaProducto;
import com.willyes.clemenintegra.bom.model.enums.EstadoFormula;
import com.willyes.clemenintegra.bom.repository.FormulaProductoRepository;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioDTO;
import com.willyes.clemenintegra.inventario.dto.MovimientoInventarioResponseDTO;
import com.willyes.clemenintegra.inventario.model.*;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.inventario.regularizacion.dto.RegularizacionTrazabilidadRequestDTO;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidad;
import com.willyes.clemenintegra.inventario.regularizacion.model.RegularizacionTrazabilidadDetalle;
import com.willyes.clemenintegra.inventario.regularizacion.service.impl.RegularizacionTrazabilidadServiceImpl;
import com.willyes.clemenintegra.inventario.repository.*;
import com.willyes.clemenintegra.inventario.service.MovimientoInventarioService;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
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

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(4L, new BigDecimal("6030"), "ACTA", "regularizacion positiva", false), "idem-pos", u);

        assertThat(respuesta.movimientos()).hasSize(1);
        assertThat(respuesta.movimientos().get(0).tipoMovimiento()).isEqualTo(TipoMovimiento.SALIDA.name());
        assertThat(respuesta.movimientos().get(0).cantidad()).isEqualByComparingTo("30");
        ArgumentCaptor<MovimientoInventarioDTO> captor = ArgumentCaptor.forClass(MovimientoInventarioDTO.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture(), anyString());
        assertThat(captor.getValue().clasificacionMovimientoInventario()).isEqualTo(ClasificacionMovimientoInventario.REGULARIZACION_TRAZABILIDAD);
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

        var respuesta = service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(8L, new BigDecimal("29900"), "ACTA", "regularizacion negativa", false), "idem-neg", u);

        assertThat(respuesta.movimientos()).hasSize(1);
        assertThat(respuesta.movimientos().get(0).tipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA.name());
        assertThat(respuesta.movimientos().get(0).cantidad()).isEqualByComparingTo("100");
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
        when(movimientoInventarioRepository.findFirstByOrdenProduccionIdAndTipoMovimientoAndClasificacionOrderByIdAsc(
                10L, TipoMovimiento.ENTRADA, ClasificacionMovimientoInventario.ENTRADA_PRODUCTO_TERMINADO
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regularizarPorOP(new RegularizacionTrazabilidadRequestDTO(10L, new BigDecimal("6030"), "ACTA", "pt sin base", true), "idem-pt", u))
                .isInstanceOf(CustomBusinessException.class)
                .extracting(e -> ((CustomBusinessException) e).getCode())
                .isEqualTo(ApiErrorCode.REGULARIZACION_PT_SIN_ENTRADA_BASE);
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
        Producto pt = new Producto();
        pt.setId(productoId);
        pt.setCategoriaProducto(new CategoriaProducto());
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
}
